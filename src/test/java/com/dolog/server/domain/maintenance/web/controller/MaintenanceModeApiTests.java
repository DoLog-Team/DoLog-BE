package com.dolog.server.domain.maintenance.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.maintenance.repository.MaintenanceModeRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class MaintenanceModeApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired MaintenanceModeRepository maintenanceModes;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    @Test
    @DisplayName("점검 모드 조회는 로그인 없이도 가능하고, 기본값은 비활성 상태다")
    void getsStatusPublicly() throws Exception {
        mvc.perform(api(get("/api/maintenance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.startAt").doesNotExist())
                .andExpect(jsonPath("$.data.endAt").doesNotExist());
    }

    @Test
    @DisplayName("두록 어드민만 점검 모드를 바꿀 수 있다 (비로그인 401, 작가 어드민 403)")
    void onlyDologAdminCanChangeStatus() throws Exception {
        mvc.perform(api(patch("/api/maintenance")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true,\"startAt\":\"2026-12-01T00:00:00\",\"endAt\":\"2026-12-01T02:00:00\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + token(Role.ARTIST_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true,\"startAt\":\"2026-12-01T00:00:00\",\"endAt\":\"2026-12-01T02:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("isActive=true로 켜면 startAt/endAt이 저장되고, 조회에도 바로 반영된다")
    void startsMaintenanceWithPeriod() throws Exception {
        String adminToken = token(Role.DOLOG_ADMIN);

        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true,\"startAt\":\"2026-12-01T00:00:00\",\"endAt\":\"2026-12-01T02:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.startAt").value("2026-12-01T00:00:00"))
                .andExpect(jsonPath("$.data.endAt").value("2026-12-01T02:00:00"));

        endRequest();
        assertTrue(maintenanceModes.findById(1L).orElseThrow().getIsActive());

        mvc.perform(api(get("/api/maintenance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.startAt").value("2026-12-01T00:00:00"));
    }

    @Test
    @DisplayName("isActive=false로 끄면 기간과 무관하게 바로 종료된다")
    void endsMaintenanceRegardlessOfPeriod() throws Exception {
        String adminToken = token(Role.DOLOG_ADMIN);

        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true,\"startAt\":\"2026-12-01T00:00:00\",\"endAt\":\"2026-12-01T02:00:00\"}"))
                .andExpect(status().isOk());

        endRequest();

        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        endRequest();
        assertFalse(maintenanceModes.findById(1L).orElseThrow().getIsActive());
    }

    @Test
    @DisplayName("isActive=true인데 시작/종료 시간이 없으면 400")
    void startingWithoutPeriodFails() throws Exception {
        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + token(Role.DOLOG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_400_1"));
    }

    @Test
    @DisplayName("종료 시간이 시작 시간보다 늦지 않으면 400")
    void invalidPeriodFails() throws Exception {
        mvc.perform(api(patch("/api/maintenance")).header("Authorization", "Bearer " + token(Role.DOLOG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true,\"startAt\":\"2026-12-01T02:00:00\",\"endAt\":\"2026-12-01T02:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_400_2"));
    }

    private String token(Role role) {
        Account account = accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/api");
    }

    private void endRequest() {
        em.flush();
        em.clear();
    }
}
