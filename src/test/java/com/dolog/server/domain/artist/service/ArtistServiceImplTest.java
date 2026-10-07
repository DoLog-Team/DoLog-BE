package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.entity.ArtistSns;
import com.dolog.server.domain.artist.exception.artistError.ArtistAccountNotEligibleException;
import com.dolog.server.domain.artist.exception.artistError.ArtistAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.like.repository.ArtistProfileLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ArtistProfileRepository artistProfileRepository;

    @Mock
    private ArtistSnsRepository artistSnsRepository;

    @Mock
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ArtistProfileLikeRepository artistProfileLikeRepository;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    @DisplayName("두록 관리자는 기존 작가 계정에 새 Artist를 연결한다")
    void createsArtistForExistingArtistAccount() {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account target = account(targetAccountId, Role.ARTIST_ADMIN, AccountStatus.ACTIVE);
        ArtistCreateRequest request = new ArtistCreateRequest(
                targetAccountId,
                " 김두록 ",
                "Dolog Kim",
                "010-1234-5678"
        );

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(accountRepository.findByIdForUpdate(targetAccountId)).thenReturn(Optional.of(target));
        when(artistRepository.findByAccountIdIncludingDeleted(targetAccountId))
                .thenReturn(Optional.empty());
        when(artistRepository.countByPhoneIncludingDeleted("01012345678"))
                .thenReturn(0L);

        artistService.createArtist(actorId, request);

        ArgumentCaptor<Artist> captor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(captor.capture());
        Artist saved = captor.getValue();
        assertSame(target, saved.getAccount());
        assertEquals("김두록", saved.getNameKo());
        assertEquals("Dolog Kim", saved.getNameEn());
        assertEquals("01012345678", saved.getPhone());
        assertNull(saved.getDeletedAt());
    }

    @Test
    @DisplayName("삭제된 Artist가 있으면 새 행 대신 같은 Artist를 복구한다")
    void restoresDeletedArtistInsteadOfCreatingNewRow() {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account target = account(targetAccountId, Role.ARTIST_ADMIN, AccountStatus.ACTIVE);
        Artist deletedArtist = Artist.builder()
                .id(artistId)
                .account(target)
                .nameKo("기존 이름")
                .nameEn("Old Name")
                .phone("01000000000")
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();
        ArtistCreateRequest request = new ArtistCreateRequest(
                targetAccountId,
                "새 이름",
                "New Name",
                "010-9999-8888"
        );

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(accountRepository.findByIdForUpdate(targetAccountId)).thenReturn(Optional.of(target));
        when(artistRepository.findByAccountIdIncludingDeleted(targetAccountId))
                .thenReturn(Optional.of(deletedArtist));
        when(artistRepository.countByPhoneIncludingDeletedAndIdNot(
                "01099998888",
                artistId
        )).thenReturn(0L);

        var response = artistService.createArtist(actorId, request);

        assertEquals(artistId, response.artistId());
        assertNull(deletedArtist.getDeletedAt());
        assertEquals("새 이름", deletedArtist.getNameKo());
        assertEquals("New Name", deletedArtist.getNameEn());
        assertEquals("01099998888", deletedArtist.getPhone());
        assertSame(target, deletedArtist.getAccount());
        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 활성 Artist가 연결된 계정은 중복 등록할 수 없다")
    void rejectsAccountWithActiveArtist() {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account target = account(targetAccountId, Role.ARTIST_ADMIN, AccountStatus.ACTIVE);
        Artist activeArtist = Artist.builder()
                .id(UUID.randomUUID())
                .account(target)
                .nameKo("기존 작가")
                .build();

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(accountRepository.findByIdForUpdate(targetAccountId)).thenReturn(Optional.of(target));
        when(artistRepository.findByAccountIdIncludingDeleted(targetAccountId))
                .thenReturn(Optional.of(activeArtist));

        assertThrows(
                ArtistAlreadyExistsException.class,
                () -> artistService.createArtist(
                        actorId,
                        new ArtistCreateRequest(targetAccountId, "작가", null, null)
                )
        );

        verify(artistRepository, never()).save(any());
    }

    @Test
    @DisplayName("ACTIVE ARTIST_ADMIN 계정이 아니면 Artist를 연결할 수 없다")
    void rejectsIneligibleTargetAccount() {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account withdrawnTarget = account(
                targetAccountId,
                Role.ARTIST_ADMIN,
                AccountStatus.WITHDRAWN
        );

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(accountRepository.findByIdForUpdate(targetAccountId))
                .thenReturn(Optional.of(withdrawnTarget));

        assertThrows(
                ArtistAccountNotEligibleException.class,
                () -> artistService.createArtist(
                        actorId,
                        new ArtistCreateRequest(targetAccountId, "작가", null, null)
                )
        );

        verify(artistRepository, never()).findByAccountIdIncludingDeleted(any());
    }

    @Test
    @DisplayName("작가 관리 서비스도 두록 관리자 권한을 검증한다")
    void rejectsNonDologAdminAtServiceBoundary() {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Account actor = account(actorId, Role.ARTIST_ADMIN, AccountStatus.ACTIVE);

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));

        assertThrows(
                AccessDeniedException.class,
                () -> artistService.createArtist(
                        actorId,
                        new ArtistCreateRequest(targetAccountId, "작가", null, null)
                )
        );

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("두록 관리자는 Artist와 Account 연결을 유지한 채 기본정보를 수정한다")
    void updatesArtistWithoutChangingAccount() {
        UUID actorId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account target = account(UUID.randomUUID(), Role.ARTIST_ADMIN, AccountStatus.ACTIVE);
        Artist artist = Artist.builder()
                .id(artistId)
                .account(target)
                .nameKo("기존 이름")
                .phone("01000000000")
                .build();
        ArtistUpdateRequest request = new ArtistUpdateRequest();
        ReflectionTestUtils.setField(request, "nameKo", " 변경 이름 ");
        ReflectionTestUtils.setField(request, "phone", "010-1111-2222");

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
        when(artistRepository.countByPhoneIncludingDeletedAndIdNot(
                "01011112222",
                artistId
        )).thenReturn(0L);

        var response = artistService.updateArtist(actorId, artistId, request);

        assertEquals(artistId, response.artistId());
        assertEquals("변경 이름", artist.getNameKo());
        assertEquals("01011112222", artist.getPhone());
        assertSame(target, artist.getAccount());
    }

    @Test
    @DisplayName("작가 삭제는 Account와 연결 데이터를 지우지 않고 Artist만 소프트 삭제한다")
    void softlyDeletesOnlyArtist() {
        UUID actorId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        Account actor = account(actorId, Role.DOLOG_ADMIN, AccountStatus.ACTIVE);
        Account target = account(UUID.randomUUID(), Role.ARTIST_ADMIN, AccountStatus.ACTIVE);
        Artist artist = Artist.builder()
                .id(artistId)
                .account(target)
                .nameKo("삭제 작가")
                .build();

        when(accountRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

        artistService.deleteArtist(actorId, artistId);

        assertNotNull(artist.getDeletedAt());
        assertSame(target, artist.getAccount());
        assertEquals(AccountStatus.ACTIVE, target.getAccountStatus());
        verify(artistRepository, never()).delete(any());
    }

    @Test
    @DisplayName("작가 상세는 최신 공개 프로필과 전시, 공개 작품, 집계값을 반환한다")
    void returnsArtistDetailInV2Format() {
        UUID artistId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID artworkId = UUID.randomUUID();

        Artist artist = Artist.builder()
                .id(artistId)
                .nameKo("김두록")
                .nameEn("Dolog Kim")
                .phone("01012345678")
                .build();
        ArtistProfile profile = ArtistProfile.builder()
                .id(profileId)
                .artist(artist)
                .nameKo("전시용 이름")
                .bio("대표 프로필 소개")
                .profileImg("https://cdn.test/profile.webp")
                .email("artist@test.com")
                .isPublic(true)
                .build();
        ArtistSns sns = ArtistSns.builder()
                .id(UUID.randomUUID())
                .artistProfile(profile)
                .platformName("instagram")
                .url("https://instagram.com/dolog")
                .build();

        ExhibitionDetail detail = ExhibitionDetail.builder()
                .title("두록 졸업전시")
                .exhibitionImg("https://cdn.test/exhibition.webp")
                .startDate(LocalDate.of(2026, 11, 28))
                .endDate(LocalDate.of(2026, 11, 29))
                .build();
        ExhibitionMap exhibitionMap = ExhibitionMap.builder()
                .address("서울시 두록구")
                .detailLocation("두록아트홀")
                .latitude("37.0")
                .longitude("127.0")
                .build();
        Exhibition exhibition = Exhibition.builder()
                .id(exhibitionId)
                .slug("dolog-2026")
                .univName("두록대학교")
                .deptName("시각디자인과")
                .isPublic(true)
                .exhibitionDetail(detail)
                .exhibitionMap(exhibitionMap)
                .build();
        ExhibitionArtistMap artistMap = ExhibitionArtistMap.builder()
                .exhibition(exhibition)
                .artist(artist)
                .build();
        Artwork artwork = Artwork.builder()
                .id(artworkId)
                .title("숨")
                .mainImg("https://cdn.test/artwork.webp")
                .build();

        when(artistRepository.findById(artistId))
                .thenReturn(Optional.of(artist));
        when(artistProfileRepository.findLatestPublicJoinedProfile(
                org.mockito.ArgumentMatchers.eq(artistId),
                any(Pageable.class)
        )).thenReturn(List.of(profile));
        when(artistSnsRepository
                .findByArtistProfileIdOrderByCreatedAtAscIdAsc(profileId))
                .thenReturn(List.of(sns));
        when(exhibitionArtistMapRepository
                .findPublicJoinedExhibitionsByArtistId(artistId))
                .thenReturn(List.of(artistMap));
        when(artworkRepository.findPublishedByArtistId(artistId))
                .thenReturn(List.of(artwork));
        when(artistProfileLikeRepository.countByArtistId(artistId))
                .thenReturn(12L);
        when(artistProfileLikeRepository.existsByArtistIdAndVisitorId(
                artistId,
                "visitor-1"
        )).thenReturn(true);
        when(artistProfileRepository.sumViewCountByArtistId(artistId))
                .thenReturn(340L);

        var response = artistService.getArtist(artistId, "visitor-1");

        assertEquals(artistId, response.getArtistId());
        assertEquals("김두록", response.getNameKo());
        assertEquals("Dolog Kim", response.getNameEn());
        assertEquals("대표 프로필 소개", response.getBio());
        assertEquals("https://cdn.test/profile.webp", response.getProfileImg());
        assertEquals("artist@test.com", response.getEmail());
        assertEquals("instagram", response.getSnsList().get(0).platformName());
        assertEquals(exhibitionId,
                response.getExhibitions().get(0).exhibitionId());
        assertEquals("두록아트홀",
                response.getExhibitions().get(0).location());
        assertEquals(artworkId, response.getArtworks().get(0).artworkId());
        assertEquals(12, response.getLikeCount());
        assertTrue(response.isLiked());
        assertEquals(340L, response.getViewCount());
    }

    @Test
    @DisplayName("대표 공개 프로필과 방문자 식별자가 없으면 nullable 필드와 liked 기본값을 반환한다")
    void returnsDefaultsWithoutRepresentativeProfileOrVisitor() {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .nameKo("김두록")
                .build();

        when(artistRepository.findById(artistId))
                .thenReturn(Optional.of(artist));
        when(artistProfileRepository.findLatestPublicJoinedProfile(
                org.mockito.ArgumentMatchers.eq(artistId),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(exhibitionArtistMapRepository
                .findPublicJoinedExhibitionsByArtistId(artistId))
                .thenReturn(List.of());
        when(artworkRepository.findPublishedByArtistId(artistId))
                .thenReturn(List.of());
        when(artistProfileLikeRepository.countByArtistId(artistId))
                .thenReturn(0L);
        when(artistProfileRepository.sumViewCountByArtistId(artistId))
                .thenReturn(0L);

        var response = artistService.getArtist(artistId, null);

        assertNull(response.getBio());
        assertNull(response.getProfileImg());
        assertNull(response.getEmail());
        assertTrue(response.getSnsList().isEmpty());
        assertTrue(response.getExhibitions().isEmpty());
        assertTrue(response.getArtworks().isEmpty());
        assertEquals(0, response.getLikeCount());
        assertFalse(response.isLiked());
        assertEquals(0L, response.getViewCount());
        verifyNoInteractions(artistSnsRepository);
        verify(artistProfileLikeRepository, never())
                .existsByArtistIdAndVisitorId(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 작가 상세 조회는 404 예외를 발생시킨다")
    void throwsWhenArtistDoesNotExist() {
        UUID artistId = UUID.randomUUID();
        when(artistRepository.findById(artistId))
                .thenReturn(Optional.empty());

        assertThrows(
                ArtistNotFoundException.class,
                () -> artistService.getArtist(artistId, null)
        );

        verifyNoInteractions(
                artistProfileRepository,
                artistSnsRepository,
                exhibitionArtistMapRepository,
                artworkRepository,
                artistProfileLikeRepository
        );
    }

    private Account account(UUID id, Role role, AccountStatus status) {
        return Account.builder()
                .id(id)
                .role(role)
                .accountStatus(status)
                .build();
    }
}
