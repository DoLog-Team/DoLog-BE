package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtistMaterialApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    @Test
    @DisplayName("본인이 등록한 작품들의 재료를 중복 없이 모아서 돌려주고, 다른 작가의 재료는 섞이지 않는다")
    void getsOwnMaterialsDeduplicatedAndIsolated() throws Exception {
        Artist me = artist("나작가");
        Artist other = artist("다른작가");

        artworkWithMaterials(me, "작품1", List.of("캔버스에 유화", "혼합재료"));
        artworkWithMaterials(me, "작품2", List.of("혼합재료", "곰돌이"));
        artworkWithMaterials(other, "남의 작품", List.of("냐냥"));
        em.flush();
        em.clear();

        mvc.perform(api(get("/api/artists/me/materials"), token(me.getAccount())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data").value(containsInAnyOrder("캔버스에 유화", "혼합재료", "곰돌이")));
    }

    @Test
    @DisplayName("작품이 없는 작가는 빈 목록을 돌려받고, 작가 어드민이 아니면 403, 미로그인은 401")
    void emptyWhenNoArtworksAndAccessRules() throws Exception {
        Artist me = artist("새내기작가");
        em.flush();

        mvc.perform(api(get("/api/artists/me/materials"), token(me.getAccount())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        Account dologAdmin = accounts.saveAndFlush(Account.builder().role(Role.DOLOG_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        mvc.perform(api(get("/api/artists/me/materials"), token(dologAdmin)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/artists/me/materials").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    private Artist artist(String name) {
        Account account = accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        return artists.saveAndFlush(Artist.builder().account(account).nameKo(name).build());
    }

    private Artwork artworkWithMaterials(Artist artist, String title, List<String> materials) {
        Artwork artwork = Artwork.builder().title(title).description("설명")
                .category("카테고리").productionEndYear(2024).build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        artwork.replaceMaterials(materials);
        return artworks.saveAndFlush(artwork);
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private <T extends MockHttpServletRequestBuilder> T api(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }
}
