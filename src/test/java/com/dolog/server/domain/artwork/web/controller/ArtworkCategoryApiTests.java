package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artwork.entity.ArtworkCategory;
import com.dolog.server.domain.artwork.repository.ArtworkCategoryRepository;
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
class ArtworkCategoryApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtworkCategoryRepository categories;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    @Test
    @DisplayName("카테고리 목록은 로그인 없이도 orderIndex 순으로 조회된다")
    void getsCategoriesPublicly() throws Exception {
        mvc.perform(api(get("/api/artworks/categories")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("회화"))
                .andExpect(jsonPath("$.data[0].orderIndex").value(1))
                .andExpect(jsonPath("$.data.length()").value(8));
    }

    @Test
    @DisplayName("두록 어드민만 카테고리를 생성/수정/삭제할 수 있다 (작가 어드민 403, 비로그인 401)")
    void onlyDologAdminCanManageCategories() throws Exception {
        mvc.perform(api(post("/api/artwork-categories")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"뉴미디어\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(api(post("/api/artwork-categories")).header("Authorization", "Bearer " + token(Role.ARTIST_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"뉴미디어\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(api(post("/api/artwork-categories")).header("Authorization", "Bearer " + token(Role.DOLOG_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"뉴미디어\"}"))
                .andExpect(status().isOk());

        endRequest();
        assertTrue(categories.existsByName("뉴미디어"));
    }

    @Test
    @DisplayName("생성, 이름 중복 거절, 수정, 삭제, 순서 변경이 끝까지 이어진다")
    void fullCrudLifecycle() throws Exception {
        String adminToken = token(Role.DOLOG_ADMIN);

        mvc.perform(api(post("/api/artwork-categories")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"뉴미디어\"}"))
                .andExpect(status().isOk());

        mvc.perform(api(post("/api/artwork-categories")).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"회화\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTWORK_024"));

        endRequest();
        ArtworkCategory created = categories.findAll().stream()
                .filter(c -> c.getName().equals("뉴미디어")).findFirst().orElseThrow();
        assertEquals(9, created.getOrderIndex());

        mvc.perform(api(patch("/api/artwork-categories/{id}", created.getId()))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"미디어아트\"}"))
                .andExpect(status().isOk());

        endRequest();
        assertEquals("미디어아트", categories.findById(created.getId()).orElseThrow().getName());

        ArtworkCategory painting = categories.findAll().stream()
                .filter(c -> c.getName().equals("회화")).findFirst().orElseThrow();
        mvc.perform(api(patch("/api/artwork-categories/reorder"))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"categoryId\":\"" + painting.getId() + "\",\"order\":9},"
                                + "{\"categoryId\":\"" + created.getId() + "\",\"order\":1}]"))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(9, categories.findById(painting.getId()).orElseThrow().getOrderIndex());
        assertEquals(1, categories.findById(created.getId()).orElseThrow().getOrderIndex());

        mvc.perform(api(delete("/api/artwork-categories/{id}", created.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        endRequest();
        assertTrue(categories.findById(created.getId()).isEmpty());
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
