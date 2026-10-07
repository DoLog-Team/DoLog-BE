package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAccessDeniedException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.repository.projection.ArtistProfileListItemProjection;
import com.dolog.server.domain.artist.support.ArtistProfileImageValidator;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.util.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistProfileServiceImplTest {

    @Mock
    private ArtistProfileRepository profileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Mock
    private FileService fileService;

    @Mock
    private ArtistSnsRepository artistSnsRepository;

    @Mock
    private BtsRepository btsRepository;

    @Mock
    private ArtistProfileImageValidator profileImageValidator;

    @InjectMocks
    private ArtistProfileServiceImpl service;

    @Test
    @DisplayName("POST 프로필 생성은 명세 필드를 저장하고 profileId를 반환한다")
    void createsProfileAndReturnsProfileId() throws Exception {
        UUID profileId = UUID.randomUUID();
        Account owner = account(Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(owner);
        Artist artist = artist(account(Role.ARTIST_ADMIN));
        ArtistProfileCreateRequest request = createRequest(
                artist.getId(),
                exhibition.getId()
        );
        MockMultipartFile profileImg = new MockMultipartFile(
                "profileImg",
                "profile.png",
                "image/png",
                new byte[]{1}
        );

        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        exhibition.getId(),
                        artist.getId(),
                        ExhibitionArtistStatus.JOINED
                )).thenReturn(true);
        when(profileRepository.existsByArtistAndExhibition(
                artist,
                exhibition
        )).thenReturn(false);
        when(fileService.uploadFile(profileImg, "artist-profiles"))
                .thenReturn("https://cdn.test/profile.webp");
        when(profileRepository.save(any(ArtistProfile.class)))
                .thenReturn(ArtistProfile.builder()
                        .id(profileId)
                        .artist(artist)
                        .exhibition(exhibition)
                        .isPublic(false)
                        .build());

        var response = service.createArtistProfile(
                request,
                profileImg
        );

        ArgumentCaptor<ArtistProfile> profileCaptor =
                ArgumentCaptor.forClass(ArtistProfile.class);
        verify(profileRepository).save(profileCaptor.capture());

        ArtistProfile savedProfile = profileCaptor.getValue();
        assertEquals(profileId, response.profileId());
        assertEquals("김두록", savedProfile.getNameKo());
        assertEquals("https://open.kakao.com/o/test",
                savedProfile.getPurchaseContactUrl());
        assertEquals("https://cdn.test/profile.webp",
                savedProfile.getProfileImg());
        assertFalse(savedProfile.isPublic());
        verify(profileImageValidator).validate(profileImg);
    }

    @Test
    @DisplayName("isPublic을 생략하면 프로필을 공개 상태로 생성한다")
    void defaultsCreatedProfileToPublic() throws Exception {
        Account owner = account(Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(owner);
        Artist artist = artist(account(Role.ARTIST_ADMIN));
        ArtistProfileCreateRequest request = createRequest(
                artist.getId(),
                exhibition.getId()
        );
        request.setIsPublic(null);

        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));
        when(artistRepository.findById(artist.getId()))
                .thenReturn(Optional.of(artist));
        when(exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        exhibition.getId(),
                        artist.getId(),
                        ExhibitionArtistStatus.JOINED
                )).thenReturn(true);
        when(profileRepository.existsByArtistAndExhibition(
                artist,
                exhibition
        )).thenReturn(false);
        when(profileRepository.save(any(ArtistProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createArtistProfile(request, null);

        ArgumentCaptor<ArtistProfile> profileCaptor =
                ArgumentCaptor.forClass(ArtistProfile.class);
        verify(profileRepository).save(profileCaptor.capture());
        assertTrue(profileCaptor.getValue().isPublic());
    }

    @Test
    @DisplayName("작가 어드민은 자신의 전시별 프로필을 수정할 수 있다")
    void artistAdminUpdatesOwnProfile() throws Exception {
        Account artistAccount = account(Role.ARTIST_ADMIN);
        ArtistProfile profile = profile(
                artist(artistAccount),
                exhibition(account(Role.EXHIBITION_ADMIN))
        );
        ArtistProfileUpdateRequest request = updateRequest();
        prepareUpdate(artistAccount, profile);

        var response = service.updateArtistProfile(
                artistAccount.getId(),
                profile.getId(),
                request,
                null
        );

        assertEquals("수정 이름", profile.getNameKo());
        assertEquals("https://open.kakao.com/o/updated",
                profile.getPurchaseContactUrl());
        assertFalse(profile.isPublic());
        assertEquals("https://cdn.test/original.webp",
                profile.getProfileImg());
        assertEquals(profile.getId(), response.profileId());
    }

    @Test
    @DisplayName("빈 profileImg part를 보내면 기존 프로필 이미지를 삭제한다")
    void emptyProfileImagePartDeletesExistingImage() throws Exception {
        Account artistAccount = account(Role.ARTIST_ADMIN);
        ArtistProfile profile = profile(
                artist(artistAccount),
                exhibition(account(Role.EXHIBITION_ADMIN))
        );
        MockMultipartFile emptyImage = new MockMultipartFile(
                "profileImg",
                "",
                "application/octet-stream",
                new byte[0]
        );
        prepareUpdate(artistAccount, profile);

        service.updateArtistProfile(
                artistAccount.getId(),
                profile.getId(),
                new ArtistProfileUpdateRequest(),
                emptyImage
        );

        assertNull(profile.getProfileImg());
        verify(fileService).deleteFile(
                "https://cdn.test/original.webp"
        );
    }

    @Test
    @DisplayName("전시 어드민은 자신이 관리하는 전시의 프로필을 수정할 수 있다")
    void exhibitionAdminUpdatesProfileInOwnExhibition() throws Exception {
        Account exhibitionAdmin = account(Role.EXHIBITION_ADMIN);
        ArtistProfile profile = profile(
                artist(account(Role.ARTIST_ADMIN)),
                exhibition(exhibitionAdmin)
        );
        prepareUpdate(exhibitionAdmin, profile);

        service.updateArtistProfile(
                exhibitionAdmin.getId(),
                profile.getId(),
                updateRequest(),
                null
        );

        assertEquals("수정 이름", profile.getNameKo());
    }

    @Test
    @DisplayName("두록 어드민은 모든 전시별 프로필을 수정할 수 있다")
    void dologAdminUpdatesAnyProfile() throws Exception {
        Account dologAdmin = account(Role.DOLOG_ADMIN);
        ArtistProfile profile = profile(
                artist(account(Role.ARTIST_ADMIN)),
                exhibition(account(Role.EXHIBITION_ADMIN))
        );
        prepareUpdate(dologAdmin, profile);

        service.updateArtistProfile(
                dologAdmin.getId(),
                profile.getId(),
                updateRequest(),
                null
        );

        assertEquals("수정 이름", profile.getNameKo());
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ARTIST_ADMIN", "EXHIBITION_ADMIN"}
    )
    @DisplayName("작가와 전시 어드민은 관리 범위 밖의 프로필을 수정할 수 없다")
    void scopedAdminsCannotUpdateOtherProfiles(Role role) {
        Account actor = account(role);
        ArtistProfile profile = profile(
                artist(account(Role.ARTIST_ADMIN)),
                exhibition(account(Role.EXHIBITION_ADMIN))
        );

        when(accountRepository.findById(actor.getId()))
                .thenReturn(Optional.of(actor));
        when(profileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));

        assertThrows(
                ArtistProfileAccessDeniedException.class,
                () -> service.updateArtistProfile(
                        actor.getId(),
                        profile.getId(),
                        updateRequest(),
                        null
                )
        );
    }

    @Test
    @DisplayName("두록 어드민은 명세 형식의 프로필 목록을 조회할 수 있다")
    void dologAdminGetsProfileList() {
        Account dologAdmin = account(Role.DOLOG_ADMIN);
        Exhibition exhibition = exhibition(
                account(Role.EXHIBITION_ADMIN)
        );
        UUID profileId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        ArtistProfileListItemProjection projection =
                profileListProjection(profileId, artistId);

        when(accountRepository.findById(dologAdmin.getId()))
                .thenReturn(Optional.of(dologAdmin));
        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));
        when(profileRepository.findListItemsByExhibitionId(
                exhibition.getId()
        )).thenReturn(List.of(projection));

        var response = service.getArtistProfileList(
                dologAdmin.getId(),
                exhibition.getId()
        );

        assertEquals(1, response.getProfiles().size());
        var item = response.getProfiles().get(0);
        assertEquals(profileId, item.getProfileId());
        assertEquals(artistId, item.getArtistId());
        assertEquals("김두록", item.getNameKo());
        assertEquals("Dolog Kim", item.getNameEn());
        assertEquals("https://cdn.test/profile.webp",
                item.getProfileImg());
        assertTrue(item.getIsPublic());
        assertEquals(120L, item.getViewCount());
        assertEquals(8, item.getLikeCount());
    }

    @Test
    @DisplayName("전시 어드민은 자신이 관리하는 전시의 프로필 목록을 조회할 수 있다")
    void exhibitionAdminGetsOwnExhibitionProfileList() {
        Account exhibitionAdmin = account(Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(exhibitionAdmin);

        when(accountRepository.findById(exhibitionAdmin.getId()))
                .thenReturn(Optional.of(exhibitionAdmin));
        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));
        when(profileRepository.findListItemsByExhibitionId(
                exhibition.getId()
        )).thenReturn(List.of());

        var response = service.getArtistProfileList(
                exhibitionAdmin.getId(),
                exhibition.getId()
        );

        assertTrue(response.getProfiles().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ARTIST_ADMIN", "EXHIBITION_ADMIN"}
    )
    @DisplayName("작가 및 다른 전시 어드민은 프로필 목록을 조회할 수 없다")
    void unauthorizedActorCannotGetProfileList(Role role) {
        Account actor = account(role);
        Exhibition exhibition = exhibition(
                account(Role.EXHIBITION_ADMIN)
        );

        when(accountRepository.findById(actor.getId()))
                .thenReturn(Optional.of(actor));
        when(exhibitionRepository.findById(exhibition.getId()))
                .thenReturn(Optional.of(exhibition));

        assertThrows(
                ArtistProfileAccessDeniedException.class,
                () -> service.getArtistProfileList(
                        actor.getId(),
                        exhibition.getId()
                )
        );

        verify(profileRepository, never())
                .findListItemsByExhibitionId(exhibition.getId());
    }

    private void prepareUpdate(
            Account actor,
            ArtistProfile profile
    ) {
        when(accountRepository.findById(actor.getId()))
                .thenReturn(Optional.of(actor));
        when(profileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));
    }

    private ArtistProfileCreateRequest createRequest(
            UUID artistId,
            UUID exhibitionId
    ) {
        ArtistProfileCreateRequest request =
                new ArtistProfileCreateRequest();
        request.setArtistId(artistId);
        request.setExhibitionId(exhibitionId);
        request.setNameKo("김두록");
        request.setNameEn("Dolog Kim");
        request.setBio("소개");
        request.setEmail("artist@test.com");
        request.setPurchaseContactUrl(
                "https://open.kakao.com/o/test"
        );
        request.setIsPublic(false);
        return request;
    }

    private ArtistProfileUpdateRequest updateRequest() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();
        request.setNameKo("수정 이름");
        request.setPurchaseContactUrl(
                "https://open.kakao.com/o/updated"
        );
        request.setIsPublic(false);
        return request;
    }

    private Account account(Role role) {
        return Account.builder()
                .id(UUID.randomUUID())
                .role(role)
                .email(UUID.randomUUID() + "@test.com")
                .build();
    }

    private Exhibition exhibition(Account owner) {
        return Exhibition.builder()
                .id(UUID.randomUUID())
                .account(owner)
                .slug("test-" + UUID.randomUUID())
                .univName("두록대학교")
                .deptName("시각디자인학과")
                .build();
    }

    private Artist artist(Account account) {
        return Artist.builder()
                .id(UUID.randomUUID())
                .account(account)
                .nameKo("김두록")
                .build();
    }

    private ArtistProfile profile(
            Artist artist,
            Exhibition exhibition
    ) {
        return ArtistProfile.builder()
                .id(UUID.randomUUID())
                .artist(artist)
                .exhibition(exhibition)
                .nameKo("기존 이름")
                .profileImg("https://cdn.test/original.webp")
                .isPublic(true)
                .build();
    }

    private ArtistProfileListItemProjection profileListProjection(
            UUID profileId,
            UUID artistId
    ) {
        ArtistProfileListItemProjection projection =
                mock(ArtistProfileListItemProjection.class);
        when(projection.getProfileId()).thenReturn(profileId.toString());
        when(projection.getArtistId()).thenReturn(artistId.toString());
        when(projection.getNameKo()).thenReturn("김두록");
        when(projection.getNameEn()).thenReturn("Dolog Kim");
        when(projection.getProfileImg())
                .thenReturn("https://cdn.test/profile.webp");
        when(projection.getIsPublic()).thenReturn(true);
        when(projection.getViewCount()).thenReturn(120L);
        when(projection.getLikeCount()).thenReturn(8L);
        return projection;
    }
}
