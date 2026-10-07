package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.entity.ArtistSns;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private ArtworkArtistMapRepository artworkArtistMapRepository;

    @Mock
    private ArtworkRepository artworkRepository;

    @Mock
    private ArtistProfileLikeRepository artistProfileLikeRepository;

    @InjectMocks
    private ArtistServiceImpl service;

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

        var response = service.getArtist(artistId, "visitor-1");

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

        var response = service.getArtist(artistId, null);

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
                () -> service.getArtist(artistId, null)
        );

        verifyNoInteractions(
                artistProfileRepository,
                artistSnsRepository,
                exhibitionArtistMapRepository,
                artworkRepository,
                artistProfileLikeRepository
        );
    }
}
