package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistPublicResponse;
import com.dolog.server.global.config.SecurityConfig;
import com.dolog.server.global.jwt.JwtAuthenticationEntryPoint;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import com.dolog.server.domain.like.support.VisitorIdResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class ArtistControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @MockitoBean
    private VisitorIdResolver visitorIdResolver;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("비로그인 사용자의 작가 목록 조회는 401을 반환한다")
    void unauthenticatedArtistListReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/artists").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JWT_401_UNAUTHORIZED"));

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("두록 관리자가 아닌 사용자의 작가 목록 조회는 403을 반환한다")
    void nonDologAdminArtistListReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/artists")
                        .contextPath("/api")
                        .with(user("artist-admin").roles("ARTIST_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GLOBAL_403"));

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("두록 관리자는 작가 목록을 조회할 수 있다")
    void dologAdminCanGetArtistList() throws Exception {
        ArtistListResponse response = ArtistListResponse.builder()
                .artists(List.of())
                .totalElements(0)
                .totalPages(0)
                .build();

        when(artistService.getArtists(null, 0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/artists")
                        .contextPath("/api")
                        .with(user("dolog-admin").roles("DOLOG_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artists").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));

        verify(artistService).getArtists(null, 0, 10);
    }

    @Test
    @DisplayName("비로그인 사용자도 v2 형식의 작가 상세를 조회할 수 있다")
    void publicArtistDetailReturnsV2Response() throws Exception {
        UUID artistId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID artworkId = UUID.randomUUID();
        ArtistPublicResponse response = ArtistPublicResponse.builder()
                .artistId(artistId)
                .nameKo("테스트 작가")
                .nameEn("Test Artist")
                .bio("작가 소개")
                .profileImg("https://cdn.test/profile.webp")
                .email("artist@test.com")
                .snsList(List.of(new ArtistPublicResponse.SnsItem(
                        "instagram",
                        "https://instagram.com/test"
                )))
                .exhibitions(List.of(new ArtistPublicResponse.ExhibitionItem(
                        exhibitionId,
                        "테스트 전시",
                        "test-exhibition",
                        "https://cdn.test/exhibition.webp",
                        "두록대학교",
                        "시각디자인과",
                        "두록아트홀",
                        LocalDate.of(2026, 11, 28),
                        LocalDate.of(2026, 11, 29)
                )))
                .artworks(List.of(new ArtistPublicResponse.ArtworkItem(
                        artworkId,
                        "숨",
                        "https://cdn.test/artwork.webp"
                )))
                .likeCount(12)
                .liked(true)
                .viewCount(340L)
                .build();

        when(visitorIdResolver.resolve(any(), isNull()))
                .thenReturn(Optional.of("visitor-1"));
        when(artistService.getArtist(artistId, "visitor-1"))
                .thenReturn(response);

        mockMvc.perform(get("/api/artists/{artistId}", artistId)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("작가 상세 조회 성공"))
                .andExpect(jsonPath("$.data.artistId").value(artistId.toString()))
                .andExpect(jsonPath("$.data.nameKo").value("테스트 작가"))
                .andExpect(jsonPath("$.data.nameEn").value("Test Artist"))
                .andExpect(jsonPath("$.data.bio").value("작가 소개"))
                .andExpect(jsonPath("$.data.profileImg").value("https://cdn.test/profile.webp"))
                .andExpect(jsonPath("$.data.email").value("artist@test.com"))
                .andExpect(jsonPath("$.data.snsList[0].platformName").value("instagram"))
                .andExpect(jsonPath("$.data.snsList[0].url").value("https://instagram.com/test"))
                .andExpect(jsonPath("$.data.snsList[0].snsId").doesNotExist())
                .andExpect(jsonPath("$.data.exhibitions[0].exhibitionId").value(exhibitionId.toString()))
                .andExpect(jsonPath("$.data.exhibitions[0].title").value("테스트 전시"))
                .andExpect(jsonPath("$.data.exhibitions[0].location").value("두록아트홀"))
                .andExpect(jsonPath("$.data.exhibitions[0].startDate").value("2026-11-28"))
                .andExpect(jsonPath("$.data.artworks[0].artworkId").value(artworkId.toString()))
                .andExpect(jsonPath("$.data.artworks[0].title").value("숨"))
                .andExpect(jsonPath("$.data.likeCount").value(12))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(340))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());

        verify(artistService).getArtist(artistId, "visitor-1");
    }
}
