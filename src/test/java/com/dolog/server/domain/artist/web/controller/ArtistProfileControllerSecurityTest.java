package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListItemResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileUpdateResponse;
import com.dolog.server.global.config.SecurityConfig;
import com.dolog.server.global.jwt.JwtAuthenticationEntryPoint;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import com.dolog.server.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class ArtistProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArtistProfileService artistProfileService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("비로그인 사용자의 관리자용 프로필 목록 조회는 401을 반환한다")
    void unauthenticatedListReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/artist-profiles")
                        .param("exhibitionId", UUID.randomUUID().toString())
                        .contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("JWT_401_UNAUTHORIZED"));

        verifyNoInteractions(artistProfileService);
    }

    @Test
    @DisplayName("작가 어드민은 관리자용 프로필 목록에 접근할 수 없다")
    void artistAdminCannotAccessList() throws Exception {
        mockMvc.perform(get("/api/artist-profiles")
                        .param("exhibitionId", UUID.randomUUID().toString())
                        .contextPath("/api")
                        .with(user(userDetails(
                                UUID.randomUUID(),
                                "ARTIST_ADMIN"
                        ))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistProfileService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "EXHIBITION_ADMIN",
            "DOLOG_ADMIN"
    })
    @DisplayName("전시·두록 어드민은 관리자용 프로필 목록 API에 접근할 수 있다")
    void adminRolesCanAccessList(String role) throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        ArtistProfileListResponse response =
                ArtistProfileListResponse.builder()
                        .profiles(List.of(
                                ArtistProfileListItemResponse.builder()
                                        .profileId(profileId)
                                        .artistId(artistId)
                                        .nameKo("김두록")
                                        .nameEn("Dolog Kim")
                                        .profileImg("https://cdn.test/profile.webp")
                                        .isPublic(true)
                                        .viewCount(120L)
                                        .likeCount(8)
                                        .build()
                        ))
                        .build();

        when(artistProfileService.getArtistProfileList(
                accountId,
                exhibitionId
        )).thenReturn(response);

        mockMvc.perform(get("/api/artist-profiles")
                        .param("exhibitionId", exhibitionId.toString())
                        .contextPath("/api")
                        .with(user(userDetails(accountId, role))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("프로필 목록 조회 성공"))
                .andExpect(jsonPath("$.data.profiles").isArray())
                .andExpect(jsonPath("$.data.profiles[0].profileId")
                        .value(profileId.toString()))
                .andExpect(jsonPath("$.data.profiles[0].artistId")
                        .value(artistId.toString()))
                .andExpect(jsonPath("$.data.profiles[0].isPublic")
                        .value(true))
                .andExpect(jsonPath("$.data.profiles[0].viewCount")
                        .value(120))
                .andExpect(jsonPath("$.data.profiles[0].likeCount")
                        .value(8))
                .andExpect(jsonPath("$.data.total").doesNotExist())
                .andExpect(jsonPath("$.data.artistProfiles")
                        .doesNotExist());

        verify(artistProfileService).getArtistProfileList(
                accountId,
                exhibitionId
        );
    }

    @Test
    @DisplayName("관리자용 프로필 목록 조회에는 exhibitionId가 필수다")
    void listRequiresExhibitionId() throws Exception {
        mockMvc.perform(get("/api/artist-profiles")
                        .contextPath("/api")
                        .with(user(userDetails(
                                UUID.randomUUID(),
                                "DOLOG_ADMIN"
                        ))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(artistProfileService);
    }

    @Test
    @DisplayName("프로필 상세 조회는 기존처럼 비로그인 사용자에게 열려 있다")
    void detailRemainsPublic() throws Exception {
        UUID profileId = UUID.randomUUID();

        mockMvc.perform(get(
                        "/api/artist-profiles/{profileId}",
                        profileId
                ).contextPath("/api"))
                .andExpect(status().isOk());

        verify(artistProfileService).getArtistProfileDetail(profileId);
    }

    @Test
    @DisplayName("비로그인 사용자의 프로필 수정은 401을 반환한다")
    void unauthenticatedUpdateReturnsUnauthorized() throws Exception {
        UUID profileId = UUID.randomUUID();

        mockMvc.perform(multipart(
                        HttpMethod.PATCH,
                        "/api/artist-profiles/{profileId}",
                        profileId
                )
                        .file(updateRequestPart())
                        .contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("JWT_401_UNAUTHORIZED"));

        verifyNoInteractions(artistProfileService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ARTIST_ADMIN",
            "EXHIBITION_ADMIN",
            "DOLOG_ADMIN"
    })
    @DisplayName("작가·전시·두록 어드민은 프로필 수정 API에 접근할 수 있다")
    void supportedRolesCanAccessUpdate(String role) throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        CustomUserDetails userDetails = userDetails(accountId, role);
        ArtistProfileUpdateResponse response =
                new ArtistProfileUpdateResponse(profileId);

        when(artistProfileService.updateArtistProfile(
                eq(accountId),
                eq(profileId),
                any(ArtistProfileUpdateRequest.class),
                eq(null)
        )).thenReturn(response);

        mockMvc.perform(multipart(
                        HttpMethod.PATCH,
                        "/api/artist-profiles/{profileId}",
                        profileId
                )
                        .file(updateRequestPart())
                        .contextPath("/api")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId")
                        .value(profileId.toString()))
                .andExpect(jsonPath("$.message")
                        .value("프로필 수정 성공"));

        verify(artistProfileService).updateArtistProfile(
                eq(accountId),
                eq(profileId),
                any(ArtistProfileUpdateRequest.class),
                eq(null)
        );
    }

    @Test
    @DisplayName("두록 어드민의 프로필 생성은 201과 profileId를 반환한다")
    void dologAdminCreatesProfile() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ArtistProfileCreateRequest request =
                new ArtistProfileCreateRequest();
        request.setArtistId(UUID.randomUUID());
        request.setExhibitionId(UUID.randomUUID());
        request.setNameKo("김두록");

        when(artistProfileService.createArtistProfile(
                any(ArtistProfileCreateRequest.class),
                eq(null)
        )).thenReturn(new ArtistProfileCreateResponse(profileId));

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/artist-profiles")
                        .file(requestPart)
                        .contextPath("/api")
                        .with(user(userDetails(
                                accountId,
                                "DOLOG_ADMIN"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED_201"))
                .andExpect(jsonPath("$.message").value("프로필 등록 성공"))
                .andExpect(jsonPath("$.data.profileId")
                        .value(profileId.toString()));
    }

    private MockMultipartFile updateRequestPart() throws Exception {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();
        request.setNameKo("수정 이름");

        return new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );
    }

    private CustomUserDetails userDetails(
            UUID accountId,
            String role
    ) {
        return new CustomUserDetails(
                accountId,
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
