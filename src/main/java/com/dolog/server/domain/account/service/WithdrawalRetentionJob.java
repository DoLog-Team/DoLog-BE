package com.dolog.server.domain.account.service;

import com.dolog.server.global.util.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class WithdrawalRetentionJob {
    private final JdbcTemplate jdbc;
    private final FileService files;
    private final TransactionTemplate transaction;

    public WithdrawalRetentionJob(JdbcTemplate jdbc, FileService files, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.files = files;
        this.transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        purgeTargets("artworks", "deleted_at", now, this::purgeArtwork);
        purgeTargets("bts", "deleted_at", now, this::purgeBts);
        purgeTargets("accounts", "withdrawn_at", now, id -> purgeAccount(id, now));
        purgeFiles();
    }

    private void purgeTargets(String table, String timestamp, LocalDateTime now, Consumer<byte[]> purge) {
        String condition = "DATE_ADD(" + timestamp + ", INTERVAL 3 MONTH) <= ?"
                + (table.equals("accounts") ? " AND account_status = 'WITHDRAWN'" : "");
        int deleted = 0;
        int failed = 0;
        for (byte[] id : jdbc.queryForList("SELECT id FROM " + table + " WHERE " + condition + " ORDER BY id",
                byte[].class, now)) {
            try {
                Boolean removed = transaction.execute(status -> {
                    // Recheck eligibility after acquiring the lock (another instance may have purged it).
                    if (jdbc.queryForList("SELECT id FROM " + table + " WHERE id = ? AND " + condition
                            + " FOR UPDATE", id, now).isEmpty()) return false;
                    purge.accept(id);
                    return true;
                });
                if (Boolean.TRUE.equals(removed)) deleted++;
            } catch (RuntimeException e) {
                failed++;
                log.error("Retention deletion failed: table={}, id={}", table, HexFormat.of().formatHex(id), e);
            }
        }
        log.info("Retention deletion: table={}, deleted={}, failed={}", table, deleted, failed);
    }

    private void purgeArtwork(byte[] id) {
        enqueue("SELECT main_img FROM artworks WHERE id = ?", id);
        enqueue("SELECT location_map FROM artworks WHERE id = ?", id);
        enqueue("SELECT image_url FROM artwork_imgs WHERE artwork_id = ?", id);
        for (String table : List.of("bts_artwork_map", "artwork_artist_maps", "artwork_imgs",
                "artwork_materials", "artwork_likes")) {
            jdbc.update("DELETE FROM " + table + " WHERE artwork_id = ?", id);
        }
        jdbc.update("DELETE FROM artworks WHERE id = ?", id);
    }

    private void purgeBts(byte[] id) {
        enqueue("SELECT main_img FROM bts WHERE id = ?", id);
        jdbc.update("DELETE FROM bts_artwork_map WHERE bts_id = ?", id);
        jdbc.update("DELETE FROM bts WHERE id = ?", id);
    }

    private void purgeAccount(byte[] accountId, LocalDateTime now) {
        for (byte[] artistId : jdbc.queryForList("SELECT id FROM artists WHERE account_id = ? ORDER BY id FOR UPDATE",
                byte[].class, accountId)) {
            // Do not remove links or profiles when dependent content has not expired or failed cleanup.
            Integer pending = jdbc.queryForObject("SELECT "
                    + "(SELECT COUNT(*) FROM artists WHERE id = ? AND (deleted_at IS NULL OR DATE_ADD(deleted_at, INTERVAL 3 MONTH) > ?)) + "
                    + "(SELECT COUNT(*) FROM artist_profiles WHERE artist_id = ? AND (deleted_at IS NULL OR DATE_ADD(deleted_at, INTERVAL 3 MONTH) > ?)) + "
                    + "(SELECT COUNT(*) FROM bts b JOIN artist_profiles p ON p.id = b.artist_profile_id WHERE p.artist_id = ?) + "
                    + "(SELECT COUNT(*) FROM artwork_artist_maps m JOIN artworks a ON a.id = m.artwork_id WHERE m.artist_id = ? AND a.deleted_at IS NOT NULL)",
                    Integer.class, artistId, now, artistId, now, artistId, artistId);
            if (pending == null || pending != 0) {
                throw new IllegalStateException("Retained artist content is not yet purged");
            }
            enqueue("SELECT profile_img FROM artist_profiles WHERE artist_id = ?", artistId);
            jdbc.update("DELETE FROM artwork_artist_maps WHERE artist_id = ?", artistId);
            jdbc.update("DELETE s FROM artist_sns s JOIN artist_profiles p ON p.id = s.artist_profile_id WHERE p.artist_id = ?", artistId);
            jdbc.update("DELETE l FROM artist_profile_likes l JOIN artist_profiles p ON p.id = l.artist_profile_id WHERE p.artist_id = ?", artistId);
            jdbc.update("DELETE FROM artist_profiles WHERE artist_id = ?", artistId);
            jdbc.update("DELETE FROM exhibition_artist_map WHERE artist_id = ?", artistId);
            jdbc.update("DELETE FROM artists WHERE id = ?", artistId);
        }
        // Notification migrations can be merged before or after this branch.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = 'notifications'", Integer.class) > 0) {
            jdbc.update("DELETE FROM notifications WHERE account_id = ?", accountId);
        }
        jdbc.update("DELETE FROM terms_agreements WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM refresh_token WHERE account_id = ?", accountId);
        jdbc.update("DELETE FROM accounts WHERE id = ?", accountId);
    }

    private void enqueue(String query, byte[] id) {
        for (String url : jdbc.queryForList(query, String.class, id)) {
            if (url != null && !url.isBlank()) {
                jdbc.update("INSERT INTO file_deletion_tasks (file_url) VALUES (?)", url);
            }
        }
    }

    private void purgeFiles() {
        int deleted = 0;
        int failed = 0;
        for (Long id : jdbc.queryForList("SELECT id FROM file_deletion_tasks ORDER BY id", Long.class)) {
            try {
                Boolean removed = transaction.execute(status -> {
                    var urls = jdbc.queryForList("SELECT file_url FROM file_deletion_tasks WHERE id = ? FOR UPDATE",
                            String.class, id);
                    if (urls.isEmpty()) return false;
                    String url = urls.get(0);
                    Integer references = jdbc.queryForObject("SELECT "
                            + "(SELECT COUNT(*) FROM artworks WHERE main_img = ? OR location_map = ?) + "
                            + "(SELECT COUNT(*) FROM artwork_imgs WHERE image_url = ?) + "
                            + "(SELECT COUNT(*) FROM bts WHERE main_img = ?) + "
                            + "(SELECT COUNT(*) FROM artist_profiles WHERE profile_img = ?)",
                            Integer.class, url, url, url, url, url);
                    if (references == null || references != 0) return false;
                    // Source deletion already committed. Only the retry task rolls back on S3 failure.
                    files.deleteOwnedFile(url);
                    jdbc.update("DELETE FROM file_deletion_tasks WHERE id = ?", id);
                    return true;
                });
                if (Boolean.TRUE.equals(removed)) deleted++;
            } catch (RuntimeException e) {
                failed++;
                log.error("Retention file deletion failed: taskId={}", id, e);
            }
        }
        log.info("Retention file deletion: deleted={}, failed={}", deleted, failed);
    }
}
