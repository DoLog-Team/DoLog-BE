package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {
    private final AccountRepository accounts;
    private final JdbcTemplate jdbc;

    @Transactional
    public void withdraw(UUID accountId) {
        var account = accounts.findForWithdrawal(accountId).orElseThrow(JwtInvalidException::new);
        if (account.getRole() != Role.ARTIST_ADMIN) {
            throw new AccessDeniedException("작가 계정만 탈퇴할 수 있습니다.");
        }
        account.requireActive();
        LocalDateTime now = LocalDateTime.now();
        byte[] id = ByteBuffer.allocate(16).putLong(accountId.getMostSignificantBits())
                .putLong(accountId.getLeastSignificantBits()).array();
        // Lock artists and their works in a stable order before testing remaining coauthors.
        // Locking the artwork row also serializes different coauthors withdrawing together.
        var artistIds = jdbc.queryForList("SELECT id FROM artists WHERE account_id = ? FOR UPDATE", byte[].class, id);
        for (byte[] artistId : artistIds) {
            var artworkIds = jdbc.queryForList("SELECT DISTINCT artwork_id FROM artwork_artist_maps "
                    + "WHERE artist_id = ? AND deleted_at IS NULL ORDER BY artwork_id", byte[].class, artistId);
            for (byte[] artworkId : artworkIds) {
                jdbc.queryForList("SELECT id FROM artworks WHERE id = ? FOR UPDATE", artworkId);
                // A locking read sees committed coauthor changes, even with MySQL REPEATABLE READ.
                var remaining = jdbc.queryForList("SELECT id FROM artwork_artist_maps "
                        + "WHERE artwork_id = ? AND artist_id <> ? AND deleted_at IS NULL FOR UPDATE",
                        artworkId, artistId);
                if (remaining.isEmpty()) {
                    jdbc.update("UPDATE artworks SET deleted_at = ? WHERE id = ? AND deleted_at IS NULL", now, artworkId);
                    jdbc.update("UPDATE bts_artwork_map SET deleted_at = ? WHERE artwork_id = ? AND deleted_at IS NULL",
                            now, artworkId);
                }
            }
            jdbc.update("UPDATE artwork_artist_maps SET deleted_at = ? WHERE artist_id = ? AND deleted_at IS NULL", now, artistId);
            jdbc.update("UPDATE bts b JOIN artist_profiles p ON p.id = b.artist_profile_id "
                    + "SET b.deleted_at = ? WHERE p.artist_id = ? AND b.deleted_at IS NULL", now, artistId);
            jdbc.update("UPDATE artist_profiles SET deleted_at = ? WHERE artist_id = ? AND deleted_at IS NULL", now, artistId);
            jdbc.update("UPDATE artists SET deleted_at = ? WHERE id = ? AND deleted_at IS NULL", now, artistId);
            jdbc.update("UPDATE exhibition_artist_map SET status = 'WITHDRAWN' WHERE artist_id = ? "
                    + "AND status IN ('PENDING', 'JOINED')", artistId);
        }
        jdbc.update("DELETE FROM refresh_token WHERE account_id = ?", id);
        account.withdraw(now);
    }
}
