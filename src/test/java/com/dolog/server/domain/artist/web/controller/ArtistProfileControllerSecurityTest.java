package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
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
