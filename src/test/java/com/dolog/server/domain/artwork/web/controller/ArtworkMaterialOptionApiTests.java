package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artwork.entity.ArtworkMaterialOption;
import com.dolog.server.domain.artwork.repository.ArtworkMaterialOptionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkMaterialOptionApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtworkMaterialOptionRepository materialOptions;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    @Test
    @DisplayName("재료 기본 옵션 목록은 로그인 없이도 orderIndex 순으로 조회되고, 마이그레이션 시딩 14개가 그대로 있다")
    void getsMaterialOptionsPublicly() throws Exception {
        mvc.perform(api(get("/api/artworks/materials")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("캔버스에 아크릴릭"))
                .andExpect(jsonPath("$.data[0].orderIndex").value(1))
                .andExpect(jsonPath("$.data.length()").value(14));
    }

    @Test
    @DisplayName("두록 어드민만 재료 옵션을 생성/수정/삭제할 수 있다 (작가 어드민 403, 비로그인 401)")
    void onlyDologAdminCanManageMaterialOptions() throws Exception {
        mvc.perform(api(post("/api/artwork-material-options")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수묵\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(api(post("/api/artwork-material-options")).header("Authorization", "Bearer " + token(Role.ARTIST_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수묵\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(api(post("/api/artwork-material-options")).header("Authorization", "Bearer " + token(Role.DOLOG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수묵\"}"))
                .andExpect(status().isOk());

        endRequest();
        assertTrue(materialOptions.existsByName("수묵"));
    }

    @Test
    @DisplayName("생성, 이름 중복 거절, 수정, 삭제, 순서 변경이 끝까지 이어진다")
    void fullCrudLifecycle() throws Exception {
        String adminToken = token(Role.DOLOG_ADMIN);

        mvc.perform(api(post("/api/artwork-material-options")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수묵\"}"))
                .andExpect(status().isOk());

        mvc.perform(api(post("/api/artwork-material-options")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"레진\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTWORK_028"));

        endRequest();
        ArtworkMaterialOption created = materialOptions.findAll().stream()
                .filter(o -> o.getName().equals("수묵")).findFirst().orElseThrow();
        assertEquals(15, created.getOrderIndex());

        mvc.perform(api(patch("/api/artwork-material-options/{id}", created.getId()))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수묵담채\"}"))
                .andExpect(status().isOk());

        endRequest();
        assertEquals("수묵담채", materialOptions.findById(created.getId()).orElseThrow().getName());

        ArtworkMaterialOption resin = materialOptions.findAll().stream()
                .filter(o -> o.getName().equals("레진")).findFirst().orElseThrow();
        mvc.perform(api(patch("/api/artwork-material-options/reorder"))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"materialOptionId\":\"" + resin.getId() + "\",\"order\":15},"
                                + "{\"materialOptionId\":\"" + created.getId() + "\",\"order\":1}]"))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(15, materialOptions.findById(resin.getId()).orElseThrow().getOrderIndex());
        assertEquals(1, materialOptions.findById(created.getId()).orElseThrow().getOrderIndex());

        mvc.perform(api(delete("/api/artwork-material-options/{id}", created.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        endRequest();
        assertTrue(materialOptions.findById(created.getId()).isEmpty());
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
