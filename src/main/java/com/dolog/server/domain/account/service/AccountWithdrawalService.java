package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.bts.repository.BtsArtworkMapRepository;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.global.exception.jwt.JwtInvalidException;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {
    private final AccountRepository accounts;
    private final JdbcTemplate jdbc;

    private final ArtistRepository artists;
    private final ArtistProfileRepository profiles;
    private final ArtworkRepository artworks;
    private final ArtworkArtistMapRepository artworkMaps;
    private final BtsRepository bts;
    private final BtsArtworkMapRepository btsMaps;
    private final RefreshTokenRepository tokens;
    private final ExhibitionArtistMapRepository exhibitionMaps;
    private final NotificationService notificationService;

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
            List<UUID> deletedArtworkIds = new ArrayList<>();
            for (byte[] artworkId : artworkIds) {
                jdbc.queryForList("SELECT id FROM artworks WHERE id = ? FOR UPDATE", artworkId);
                // A locking read sees committed coauthor changes, even with MySQL REPEATABLE READ.
                var remaining = jdbc.queryForList("SELECT id FROM artwork_artist_maps "
                        + "WHERE artwork_id = ? AND artist_id <> ? AND deleted_at IS NULL FOR UPDATE",
                        artworkId, artistId);
                if (remaining.isEmpty()) {
                    deletedArtworkIds.add(uuid(artworkId));
                }
            }
            if (!deletedArtworkIds.isEmpty()) {
                artworks.hideByIds(deletedArtworkIds, now);
                btsMaps.hideByArtworkIds(deletedArtworkIds, now);
            }
            UUID artistUuid = uuid(artistId);

            // 알림은 탈퇴 처리로 상태가 바뀌기 전에, 소속 중이던 전시를 미리 조회해둔다.
            List<ExhibitionArtistMap> exhibitionsToNotify = exhibitionMaps.findAllByArtistIdAndStatusIn(
                    artistUuid, List.of(ExhibitionArtistStatus.PENDING, ExhibitionArtistStatus.JOINED));
            String artistName = artists.findById(artistUuid).map(Artist::getNameKo).orElse("");

            artworkMaps.hideByArtistId(artistUuid, now);
            bts.hideByArtistId(artistUuid, now);
            profiles.hideByArtistId(artistUuid, now);
            // State-policy unification is tracked separately from this retention change.
            exhibitionMaps.updateStatusesForWithdrawal(artistUuid,
                    List.of(ExhibitionArtistStatus.PENDING, ExhibitionArtistStatus.JOINED),
                    ExhibitionArtistStatus.WITHDRAWN, now);
            artists.hideById(artistUuid, now);

            for (ExhibitionArtistMap map : exhibitionsToNotify) {
                Exhibition exhibition = map.getExhibition();
                notificationService.send(
                        exhibition.getAccount(),
                        NotificationType.ARTIST_ACCOUNT_DELETED,
                        Map.of("artistName", artistName, "exhibitionName", resolveExhibitionName(exhibition)),
                        exhibition.getId()
                );
            }
        }
        tokens.deleteAllByAccountId(accountId);
        account.withdraw(now);
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private static String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }

    private static UUID uuid(byte[] value) {
        var buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
