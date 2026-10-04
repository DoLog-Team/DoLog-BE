package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.service.ExhibitionArtistService;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageItemResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageListResponse;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

@WebMvcTest(ExhibitionArtistController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class ExhibitionArtistControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExhibitionArtistService exhibitionArtistService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    // @WebMvcTest에는 JPA 엔티티가 없으므로 애플리케이션의 JPA Auditing 의존성만 대체한다.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("비로그인 사용자는 관리자용 전시 작가 목록 조회 시 403을 받는다")
    void anonymousCannotGetArtistsForManagement() throws Exception {
        UUID exhibitionId = UUID.randomUUID();

        mockMvc.perform(get(
                        "/api/exhibitions/{exhibitionId}/artists/manage",
                        exhibitionId
                )
                        .contextPath("/api")
                        .param("status", "PENDING"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exhibitionArtistService);
    }

    @Test
    @DisplayName("작가 관리자는 관리자용 전시 작가 목록을 조회할 수 없다")
    void artistAdminCannotGetArtistsForManagement() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();

        mockMvc.perform(get(
                        "/api/exhibitions/{exhibitionId}/artists/manage",
                        exhibitionId
                )
                        .contextPath("/api")
                        .param("status", "PENDING")
                        .with(user(userDetails(accountId, Role.ARTIST_ADMIN))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exhibitionArtistService);
    }

    @Test
    @DisplayName("전시 관리자는 관리자용 전시 작가 목록을 조회할 수 있다")
    void exhibitionAdminCanGetArtistsForManagement() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        UUID artistId = UUID.randomUUID();
        ExhibitionArtistManageListResponse serviceResponse =
                new ExhibitionArtistManageListResponse(
                        List.of(new ExhibitionArtistManageItemResponse(
                                artistId,
                                "김두록",
                                "dolog@gmail.com",
                                "2021112489 / 김두록입니다.",
                                2
                        )),
                        1,
                        1
                );

        when(exhibitionArtistService.getArtistsForManagement(
                accountId,
                exhibitionId,
                ExhibitionArtistStatus.PENDING,
                null,
                0,
                10
        )).thenReturn(serviceResponse);

        mockMvc.perform(get(
                        "/api/exhibitions/{exhibitionId}/artists/manage",
                        exhibitionId
                )
                        .contextPath("/api")
                        .param("status", "PENDING")
                        .with(user(userDetails(accountId, Role.EXHIBITION_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artists[0].artistId").value(artistId.toString()))
                .andExpect(jsonPath("$.data.artists[0].nameKo").value("김두록"))
                .andExpect(jsonPath("$.data.artists[0].email").value("dolog@gmail.com"))
                .andExpect(jsonPath("$.data.artists[0].greeting").value("2021112489 / 김두록입니다."))
                .andExpect(jsonPath("$.data.artists[0].artworkCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalElements").doesNotExist())
                .andExpect(jsonPath("$.data.artists[0].status").doesNotExist());

        verify(exhibitionArtistService).getArtistsForManagement(
                accountId,
                exhibitionId,
                ExhibitionArtistStatus.PENDING,
                null,
                0,
                10
        );
    }

    @Test
    @DisplayName("두록 관리자는 관리자용 전시 작가 목록을 조회할 수 있다")
    void dologAdminCanGetArtistsForManagement() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionArtistManageListResponse serviceResponse = emptyResponse();

        when(exhibitionArtistService.getArtistsForManagement(
                accountId,
                exhibitionId,
                ExhibitionArtistStatus.PENDING,
                null,
                0,
                10
        )).thenReturn(serviceResponse);

        mockMvc.perform(get(
                        "/api/exhibitions/{exhibitionId}/artists/manage",
                        exhibitionId
                )
                        .contextPath("/api")
                        .param("status", "PENDING")
                        .with(user(userDetails(accountId, Role.DOLOG_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0));

        verify(exhibitionArtistService).getArtistsForManagement(
                accountId,
                exhibitionId,
                ExhibitionArtistStatus.PENDING,
                null,
                0,
                10
        );
    }

    @Test
    @DisplayName("관리자용 전시 작가 목록 조회 시 참여 상태는 필수다")
    void statusIsRequiredForManagementList() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID exhibitionId = UUID.randomUUID();

        mockMvc.perform(get(
                        "/api/exhibitions/{exhibitionId}/artists/manage",
                        exhibitionId
                )
                        .contextPath("/api")
                        .with(user(userDetails(accountId, Role.EXHIBITION_ADMIN))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(exhibitionArtistService);
    }

    private CustomUserDetails userDetails(UUID accountId, Role role) {
        return new CustomUserDetails(
                accountId,
                1L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private ExhibitionArtistManageListResponse emptyResponse() {
        return new ExhibitionArtistManageListResponse(
                List.of(),
                0,
                0
        );
    }
}
