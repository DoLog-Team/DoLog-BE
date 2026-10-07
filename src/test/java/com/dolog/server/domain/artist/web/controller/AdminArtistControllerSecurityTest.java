package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistUpdateResponse;
import com.dolog.server.global.config.SecurityConfig;
import com.dolog.server.global.jwt.JwtAuthenticationEntryPoint;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import com.dolog.server.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminArtistController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class AdminArtistControllerSecurityTest {

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
    @DisplayName("비로그인 사용자는 관리자 작가 등록 API를 호출할 수 없다")
    void unauthenticatedCreateIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/artists")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("ARTIST_ADMIN은 관리자 작가 등록 API를 호출할 수 없다")
    void artistAdminCannotCreateArtist() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();

        mockMvc.perform(post("/api/admin/artists")
                        .contextPath("/api")
                        .with(user(userDetails(actorId, Role.ARTIST_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "nameKo": "김두록"
                                }
                                """.formatted(targetAccountId)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("두록 관리자는 accountId로 작가를 등록한다")
    void dologAdminCreatesArtistWithAccountId() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();

        when(artistService.createArtist(eq(actorId), any()))
                .thenReturn(new ArtistCreateResponse(artistId));

        mockMvc.perform(post("/api/admin/artists")
                        .contextPath("/api")
                        .with(user(userDetails(actorId, Role.DOLOG_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "nameKo": "김두록",
                                  "nameEn": "Dolog Kim",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(targetAccountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("작가 등록 성공"))
                .andExpect(jsonPath("$.data.artistId").value(artistId.toString()));

        verify(artistService).createArtist(eq(actorId), any());
    }

    @Test
    @DisplayName("작가 등록 요청에서 accountId가 누락되면 400을 반환한다")
    void missingAccountIdIsBadRequest() throws Exception {
        UUID actorId = UUID.randomUUID();

        mockMvc.perform(post("/api/admin/artists")
                        .contextPath("/api")
                        .with(user(userDetails(actorId, Role.DOLOG_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nameKo\":\"김두록\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(artistService);
    }

    @Test
    @DisplayName("두록 관리자는 작가 기본정보를 수정한다")
    void dologAdminUpdatesArtist() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();

        when(artistService.updateArtist(eq(actorId), eq(artistId), any()))
                .thenReturn(new ArtistUpdateResponse(artistId));

        mockMvc.perform(patch("/api/admin/artists/{artistId}", artistId)
                        .contextPath("/api")
                        .with(user(userDetails(actorId, Role.DOLOG_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"010-9999-8888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("작가 수정 성공"))
                .andExpect(jsonPath("$.data.artistId").value(artistId.toString()));
    }

    @Test
    @DisplayName("두록 관리자는 작가를 소프트 삭제한다")
    void dologAdminDeletesArtist() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/artists/{artistId}", artistId)
                        .contextPath("/api")
                        .with(user(userDetails(actorId, Role.DOLOG_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("작가 삭제 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(artistService).deleteArtist(actorId, artistId);
    }

    private CustomUserDetails userDetails(UUID accountId, Role role) {
        return new CustomUserDetails(
                accountId,
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
