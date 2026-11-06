package com.dolog.server.domain.exhibition.support;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionPublicityNotifierTest {

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ExhibitionDetailRepository exhibitionDetailRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExhibitionPublicityNotifier notifier;

    @Test
    @DisplayName("전시에만 공개인 작품의 작가에게만 알리고, 같은 작가는 한 번만 알린다")
    void notifiesOnlyExhibitionOnlyArtistsOnce() {
        UUID exhibitionId = UUID.randomUUID();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).slug("diag").build();

        Account account = Account.builder().id(UUID.randomUUID()).build();
        Artist artist = Artist.builder().account(account).build();

        Artwork exhibitionOnlyArtwork1 = Artwork.builder().id(UUID.randomUUID()).exhibition(exhibition).build();
        exhibitionOnlyArtwork1.changeStatus(ArtworkStatus.PUBLISHED);
        exhibitionOnlyArtwork1.updateExhibitionOnly(true);
        exhibitionOnlyArtwork1.getArtworkArtistMaps()
                .add(ArtworkArtistMap.builder().artwork(exhibitionOnlyArtwork1).artist(artist).build());

        // 같은 작가의 또 다른 "전시에만 공개" 작품 — 중복 알림 없이 한 번만 가야 한다
        Artwork exhibitionOnlyArtwork2 = Artwork.builder().id(UUID.randomUUID()).exhibition(exhibition).build();
        exhibitionOnlyArtwork2.changeStatus(ArtworkStatus.PUBLISHED);
        exhibitionOnlyArtwork2.updateExhibitionOnly(true);
        exhibitionOnlyArtwork2.getArtworkArtistMaps()
                .add(ArtworkArtistMap.builder().artwork(exhibitionOnlyArtwork2).artist(artist).build());

        when(artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED))
                .thenReturn(List.of(exhibitionOnlyArtwork1, exhibitionOnlyArtwork2));
        when(exhibitionDetailRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());

        notifier.notify(exhibition, false);

        verify(notificationService, times(1))
                .send(eq(account), eq(NotificationType.EXHIBITION_UNPUBLISHED), any(), eq(exhibitionId));
    }

    @Test
    @DisplayName("전시에만 공개인 작품이 없으면 알리지 않는다")
    void doesNotNotifyWhenNoExhibitionOnlyArtworks() {
        UUID exhibitionId = UUID.randomUUID();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).slug("diag2").build();

        Artwork normalArtwork = Artwork.builder().id(UUID.randomUUID()).exhibition(exhibition).build();
        normalArtwork.changeStatus(ArtworkStatus.PUBLISHED);

        when(artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED))
                .thenReturn(List.of(normalArtwork));

        notifier.notify(exhibition, false);

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("재공개(nowPublic=true)면 EXHIBITION_REPUBLISHED 타입으로 보낸다")
    void notifiesRepublishedTypeWhenNowPublic() {
        UUID exhibitionId = UUID.randomUUID();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).slug("diag3").build();

        Account account = Account.builder().id(UUID.randomUUID()).build();
        Artist artist = Artist.builder().account(account).build();
        Artwork artwork = Artwork.builder().id(UUID.randomUUID()).exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.updateExhibitionOnly(true);
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(artist).build());

        when(artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED))
                .thenReturn(List.of(artwork));
        when(exhibitionDetailRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());

        notifier.notify(exhibition, true);

        verify(notificationService, never())
                .send(any(), eq(NotificationType.EXHIBITION_UNPUBLISHED), any(), any());
        verify(notificationService)
                .send(eq(account), eq(NotificationType.EXHIBITION_REPUBLISHED), any(), eq(exhibitionId));
    }
}
