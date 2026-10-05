package com.dolog.server.domain.account.service;

import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WithdrawalRetentionJob {
    private final JdbcTemplate jdbc;
    private final FileService files;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        // Lock retained rows so concurrent app instances cannot purge the same content.
        jdbc.queryForList("SELECT id FROM accounts WHERE account_status = 'WITHDRAWN' "
                + "AND DATE_ADD(withdrawn_at, INTERVAL 3 MONTH) <= ? ORDER BY id FOR UPDATE", now);
        jdbc.queryForList("SELECT id FROM artworks WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ? ORDER BY id FOR UPDATE", now);
        Set<String> urls = new LinkedHashSet<>();
        for (String query : new String[]{
                "SELECT main_img FROM artworks WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "SELECT location_map FROM artworks WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "SELECT i.image_url FROM artwork_imgs i JOIN artworks a ON a.id = i.artwork_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ?",
                "SELECT main_img FROM bts WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "SELECT profile_img FROM artist_profiles WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?"}) {
            urls.addAll(jdbc.queryForList(query, String.class, now));
        }
        // Notifications may land before or after this branch (V10). Respect its account FK.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = 'notifications'", Integer.class) > 0) {
            jdbc.update("DELETE n FROM notifications n JOIN accounts a ON a.id = n.account_id "
                    + "WHERE a.account_status = 'WITHDRAWN' AND DATE_ADD(a.withdrawn_at, INTERVAL 3 MONTH) <= ?", now);
        }
        // Remove references first; shared works and other authors' BTS bodies are retained.
        for (String query : new String[]{
                "DELETE m FROM bts_artwork_map m LEFT JOIN artworks a ON a.id = m.artwork_id LEFT JOIN bts b ON b.id = m.bts_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ? OR DATE_ADD(b.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE FROM bts WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE m FROM artwork_artist_maps m LEFT JOIN artworks a ON a.id = m.artwork_id LEFT JOIN artists ar ON ar.id = m.artist_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ? OR DATE_ADD(ar.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE i FROM artwork_imgs i JOIN artworks a ON a.id = i.artwork_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE m FROM artwork_materials m JOIN artworks a ON a.id = m.artwork_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE l FROM artwork_likes l JOIN artworks a ON a.id = l.artwork_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE FROM artworks WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE s FROM artist_sns s JOIN artist_profiles p ON p.id = s.artist_profile_id WHERE DATE_ADD(p.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE l FROM artist_profile_likes l JOIN artist_profiles p ON p.id = l.artist_profile_id WHERE DATE_ADD(p.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE FROM artist_profiles WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE m FROM exhibition_artist_map m JOIN artists a ON a.id = m.artist_id WHERE DATE_ADD(a.deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE FROM artists WHERE DATE_ADD(deleted_at, INTERVAL 3 MONTH) <= ?",
                "DELETE t FROM terms_agreements t JOIN accounts a ON a.id = t.account_id WHERE a.account_status = 'WITHDRAWN' AND DATE_ADD(a.withdrawn_at, INTERVAL 3 MONTH) <= ?",
                "DELETE t FROM refresh_token t JOIN accounts a ON a.id = t.account_id WHERE a.account_status = 'WITHDRAWN' AND DATE_ADD(a.withdrawn_at, INTERVAL 3 MONTH) <= ?",
                "DELETE FROM accounts WHERE account_status = 'WITHDRAWN' AND DATE_ADD(withdrawn_at, INTERVAL 3 MONTH) <= ?"}) {
            if (query.indexOf('?') != query.lastIndexOf('?')) {
                jdbc.update(query, now, now);
            } else {
                jdbc.update(query, now);
            }
        }
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            // Never delete a file still referenced by another author or a retained row.
            Integer references = jdbc.queryForObject("SELECT "
                    + "(SELECT COUNT(*) FROM artworks WHERE main_img = ? OR location_map = ?) + "
                    + "(SELECT COUNT(*) FROM artwork_imgs WHERE image_url = ?) + "
                    + "(SELECT COUNT(*) FROM bts WHERE main_img = ?) + "
                    + "(SELECT COUNT(*) FROM artist_profiles WHERE profile_img = ?)",
                    Integer.class, url, url, url, url, url);
            if (references != null && references == 0) files.deleteOwnedFile(url);
        }
    }
}
