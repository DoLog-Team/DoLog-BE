package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistPublicResponse;
import com.dolog.server.global.config.SecurityConfig;
import com.dolog.server.global.jwt.JwtAuthenticationEntryPoint;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

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
    @DisplayName("비로그인 작가 상세 조회 응답에는 전화번호가 포함되지 않는다")
    void publicArtistDetailDoesNotExposePhone() throws Exception {
        UUID artistId = UUID.randomUUID();
        ArtistPublicResponse response = ArtistPublicResponse.builder()
                .id(artistId)
                .nameKo("테스트 작가")
                .nameEn("Test Artist")
                .build();

        when(artistService.getArtist(artistId)).thenReturn(response);

        mockMvc.perform(get("/api/artists/{artistId}", artistId)
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(artistId.toString()))
                .andExpect(jsonPath("$.data.nameKo").value("테스트 작가"))
                .andExpect(jsonPath("$.data.nameEn").value("Test Artist"))
                .andExpect(jsonPath("$.data.phone").doesNotExist());

        verify(artistService).getArtist(artistId);
    }
}
