package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionCustomThemeRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.host.HostRepository;
import com.dolog.server.domain.exhibition.support.ExhibitionPublishRequirement;
import com.dolog.server.domain.exhibition.web.dto.response.basic.ArtistJoinCodeResponse;
import com.dolog.server.domain.exhibition.web.dto.response.basic.ExhibitionAdminHomeResponse;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import com.dolog.server.global.util.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExhibitionServiceImplTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ExhibitionDetailRepository exhibitionDetailRepository;

    @Mock
    private ExhibitionMapRepository exhibitionMapRepository;

    @Mock
    private ExhibitionCustomThemeRepository exhibitionCustomThemeRepository;

    @Mock
    private FileService fileService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ExhibitionPublishRequirement exhibitionPublishRequirement;

    @Mock
    private HostRepository hostRepository;

    @InjectMocks
    private ExhibitionServiceImpl service;

    @Test
    @DisplayName("작가 참여 코드를 허용된 문자로 구성된 8자리 코드로 재발급한다")
    void reissuesArtistJoinCode() {
        UUID exhibitionId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        Exhibition exhibition = Exhibition.builder()
                .id(exhibitionId)
                .artistJoinCode("2345ABCD")
                .build();

        when(exhibitionRepository.findByIdForUpdate(exhibitionId))
                .thenReturn(Optional.of(exhibition));
        when(exhibitionRepository.existsByEntryCodeOrArtistJoinCode(
                anyString(),
                anyString()
        )).thenReturn(false);

        ArtistJoinCodeResponse response =
                service.reissueArtistJoinCode(exhibitionId, expiresAt);

        assertTrue(response.artistJoinCode()
                .matches("[2-9A-HJ-KM-NP-Z]{8}"));
        assertEquals(expiresAt, response.artistJoinCodeExpiresAt());
        assertEquals(response.artistJoinCode(), exhibition.getArtistJoinCode());
        assertEquals(expiresAt, exhibition.getArtistJoinCodeExpiresAt());
        verify(exhibitionRepository).existsByEntryCodeOrArtistJoinCode(
                response.artistJoinCode(),
                response.artistJoinCode()
        );
    }

    @Test
    @DisplayName("과거 만료 시각으로는 작가 참여 코드를 재발급할 수 없다")
    void rejectsPastArtistJoinCodeExpiry() {
        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.reissueArtistJoinCode(
                        UUID.randomUUID(),
                        LocalDateTime.now().minusMinutes(1)
                )
        );

        assertEquals(
                ExhibitionErrorCode.ARTIST_JOIN_CODE_EXPIRY_INVALID,
                exception.getErrorCode()
        );
        verifyNoInteractions(exhibitionRepository);
    }

    @Test
    @DisplayName("전시 어드민 홈은 공개+안 숨긴 작품 수와 숨긴 작품 수를 실제로 집계한다")
    void getAdminHomeCountsArtworks() {
        UUID exhibitionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account owner = Account.builder().id(accountId).build();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).account(owner).build();

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(exhibitionDetailRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(eq(exhibitionId), any()))
                .thenReturn(Optional.empty());
        when(exhibitionArtistMapRepository.countByExhibitionIdAndStatus(exhibitionId, ExhibitionArtistStatus.JOINED))
                .thenReturn(3L);
        when(exhibitionArtistMapRepository.countByExhibitionIdAndStatus(exhibitionId, ExhibitionArtistStatus.PENDING))
                .thenReturn(1L);
        when(artworkRepository.countByExhibitionIdAndStatusAndHiddenAtIsNull(exhibitionId, ArtworkStatus.PUBLISHED))
                .thenReturn(7L);
        when(artworkRepository.countByExhibitionIdAndHiddenAtIsNotNull(exhibitionId))
                .thenReturn(2L);
        when(exhibitionMapRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(hostRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionPublishRequirement.missingFields(exhibition, null, null, null))
                .thenReturn(java.util.List.of("address", "hostName"));

        ExhibitionAdminHomeResponse response = service.getAdminHome(exhibitionId, accountId);

        assertEquals(7, response.getPublishedArtworkCount());
        assertEquals(2, response.getHiddenArtworkCount());
        assertEquals(java.util.List.of("address", "hostName"), response.getMissingPublishFields());
    }

    @Test
    @DisplayName("게시 필수값이 비어있으면 게시를 거절하고 구독은 조회하지 않는다")
    void rejectsPublishWhenFieldsMissing() {
        UUID exhibitionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account owner = Account.builder().id(accountId).build();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).account(owner).build();

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(exhibitionDetailRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionMapRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(hostRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionPublishRequirement.isSatisfied(exhibition, null, null, null)).thenReturn(false);

        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.publishExhibition(exhibitionId, accountId)
        );

        assertEquals(ExhibitionErrorCode.EXHIBITION_PUBLISH_FIELDS_MISSING, exception.getErrorCode());
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("게시 필수값을 다 채우면 활성 구독이 없어도 만료일 없이 게시된다")
    void publishesWithoutActiveSubscription() {
        UUID exhibitionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account owner = Account.builder().id(accountId).build();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).account(owner).build();

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        when(exhibitionDetailRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionMapRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(hostRepository.findByExhibitionId(exhibitionId)).thenReturn(Optional.empty());
        when(exhibitionPublishRequirement.isSatisfied(exhibition, null, null, null)).thenReturn(true);
        when(subscriptionRepository.findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(eq(exhibitionId), any()))
                .thenReturn(Optional.empty());

        service.publishExhibition(exhibitionId, accountId);

        assertTrue(exhibition.isPublic());
        assertEquals(null, exhibition.getExpiresAt());
    }

    @Test
    @DisplayName("게시 기간 연장은 과거 시각도 허용한다 (전시 종료 후 기록 정리, 즉시 만료 처리 등)")
    void extendsExpiresAtWithPastTime() {
        UUID exhibitionId = UUID.randomUUID();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).publishedAt(LocalDateTime.now().minusDays(1)).build();
        LocalDateTime pastExpiresAt = LocalDateTime.now().minusHours(1);

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));

        service.extendExpiresAt(exhibitionId, pastExpiresAt);

        assertEquals(pastExpiresAt, exhibition.getExpiresAt());
    }

    @Test
    @DisplayName("게시되지 않은 전시는 게시 기간을 연장할 수 없다")
    void rejectsExtendExpiresAtWhenNotPublished() {
        UUID exhibitionId = UUID.randomUUID();
        Exhibition exhibition = Exhibition.builder().id(exhibitionId).build();

        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));

        ExhibitionException exception = assertThrows(
                ExhibitionException.class,
                () -> service.extendExpiresAt(exhibitionId, LocalDateTime.now().plusDays(1))
        );

        assertEquals(ExhibitionErrorCode.EXHIBITION_NOT_PUBLISHED, exception.getErrorCode());
    }
}
