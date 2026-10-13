package com.dolog.server.domain.artwork.web.controller;

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
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class FieldSettingsApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionFieldSettingsRepository fieldSettings;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    private static final String ALL_FALSE = settingsJson(false, false, false, false, false, false, false, false, false);

    private Exhibition exhibition;
    private String ownerToken;
    private Artist artist;

    @BeforeEach
    void setUp() {
        Account owner = account(Role.EXHIBITION_ADMIN);
        ownerToken = token(owner);
        exhibition = exhibition(owner);
        artist = artists.saveAndFlush(Artist.builder().account(account(Role.ARTIST_ADMIN)).nameKo("작가").build());
    }

    @Test
    @DisplayName("설정이 없는 전시는 모든 항목이 false 인 기본값을 돌려주고 행은 만들지 않는다")
    void defaultsWhenNoSettings() throws Exception {
        mvc.perform(withToken(get("/api/exhibitions/{id}/field-settings", exhibition.getId()), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required.mainImg").value(false))
                .andExpect(jsonPath("$.data.required.size").value(false))
                .andExpect(jsonPath("$.data.hidden.productionYear").value(false));

        assertTrue(fieldSettings.findByExhibitionId(exhibition.getId()).isEmpty());
    }

    @Test
    @DisplayName("저장한 값이 조회되고, 다시 저장하면 같은 행을 고친다")
    void savesAndUpdatesSingleRow() throws Exception {
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(true, false, true, false, true, false, false, true, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        endRequest();
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(false, true, true, false, false, false, true, true, false)))
                .andExpect(status().isOk());
        endRequest();

        mvc.perform(withToken(get("/api/exhibitions/{id}/field-settings", exhibition.getId()), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required.mainImg").value(false))
                .andExpect(jsonPath("$.data.required.size").value(true))
                .andExpect(jsonPath("$.data.required.materials").value(true))
                .andExpect(jsonPath("$.data.hidden.size").value(false))
                .andExpect(jsonPath("$.data.hidden.locationMap").value(true))
                .andExpect(jsonPath("$.data.hidden.productionPeriod").value(true));
        assertEquals(1L, em.createQuery("SELECT COUNT(s) FROM ExhibitionFieldSettings s WHERE s.exhibition.id = :id", Long.class)
                .setParameter("id", exhibition.getId()).getSingleResult());
    }

    @Test
    @DisplayName("필수 항목을 강화하면 못 채운 공개 작품만 비공개로 내려가고, 비공개 작품과 다른 전시 작품은 그대로다")
    void strengtheningDraftsUnsatisfiedPublishedArtworks() throws Exception {
        Artwork missing = artwork(exhibition, ArtworkStatus.PUBLISHED, null);
        Artwork satisfied = artwork(exhibition, ArtworkStatus.PUBLISHED, "https://s3/main.webp");
        Artwork draft = artwork(exhibition, ArtworkStatus.DRAFT, null);
        Exhibition other = exhibition(account(Role.EXHIBITION_ADMIN));
        Artwork otherExhibition = artwork(other, ArtworkStatus.PUBLISHED, null);

        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(true, false, false, false, false, false, false, false, false)))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(ArtworkStatus.DRAFT, statusOf(missing));
        assertEquals(ArtworkStatus.PUBLISHED, statusOf(satisfied));
        assertEquals(ArtworkStatus.DRAFT, statusOf(draft));
        assertEquals(ArtworkStatus.PUBLISHED, statusOf(otherExhibition));
    }

    @Test
    @DisplayName("필수 항목을 완화하면 그 설정 때문에 자동 비공개됐던 작품은 다시 공개된다")
    void relaxingRepublishesAutoDrafted() throws Exception {
        Artwork missing = artwork(exhibition, ArtworkStatus.PUBLISHED, null);
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(true, false, false, false, false, false, false, false, false)))
                .andExpect(status().isOk());
        endRequest();
        assertEquals(ArtworkStatus.DRAFT, statusOf(missing));

        mvc.perform(withToken(put(exhibition.getId()), ownerToken).content(ALL_FALSE))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(ArtworkStatus.PUBLISHED, statusOf(missing));
    }

    @Test
    @DisplayName("필수 항목을 완화해도 작가가 직접 비공개로 둔 작품은 다시 공개되지 않는다")
    void relaxingDoesNotRepublishManualDraft() throws Exception {
        Artwork manualDraft = artwork(exhibition, ArtworkStatus.DRAFT, null);
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(true, false, false, false, false, false, false, false, false)))
                .andExpect(status().isOk());
        endRequest();

        mvc.perform(withToken(put(exhibition.getId()), ownerToken).content(ALL_FALSE))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(ArtworkStatus.DRAFT, statusOf(manualDraft));
    }

    @Test
    @DisplayName("저장한 필수 항목은 작가의 공개 전환에도 적용된다")
    void savedSettingsApplyToPublish() throws Exception {
        Artwork artwork = artwork(exhibition, ArtworkStatus.DRAFT, null);
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(false, false, false, true, false, false, false, false, false)))
                .andExpect(status().isOk());
        endRequest();

        mvc.perform(withToken(patch("/api/artworks/{id}/exhibition", artwork.getId()), token(artist.getAccount()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_013"));
    }

    @Test
    @DisplayName("항목이 하나라도 빠지거나 required/hidden 이 없으면 400")
    void rejectsIncompleteBody() throws Exception {
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content("{\"required\":{\"mainImg\":true,\"size\":false,\"materials\":false},"
                                + "\"hidden\":{\"size\":false,\"materials\":false,\"locationMap\":false,"
                                + "\"productionPeriod\":false,\"productionYear\":false}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content("{\"required\":{\"mainImg\":true,\"size\":false,\"materials\":false,\"locationMap\":false}}"))
                .andExpect(status().isBadRequest());

        assertTrue(fieldSettings.findByExhibitionId(exhibition.getId()).isEmpty());
    }

    @Test
    @DisplayName("다른 전시 어드민은 403, 작가 어드민은 403, 미로그인은 401, 없는 전시는 404")
    void accessRules() throws Exception {
        Account otherOwnerAccount = account(Role.EXHIBITION_ADMIN);
        exhibition(otherOwnerAccount);
        String otherOwner = token(otherOwnerAccount);

        mvc.perform(withToken(get("/api/exhibitions/{id}/field-settings", exhibition.getId()), otherOwner))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_014"));
        mvc.perform(withToken(put(exhibition.getId()), otherOwner).content(ALL_FALSE))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(get("/api/exhibitions/{id}/field-settings", exhibition.getId()), token(artist.getAccount())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/exhibitions/{id}/field-settings", exhibition.getId()).contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(exhibition.getId()).contextPath("/api").content(ALL_FALSE))
                .andExpect(status().isUnauthorized());
        mvc.perform(withToken(get("/api/exhibitions/{id}/field-settings", UUID.randomUUID()), ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXHIBITION_404_1"));
    }

    @Test
    @DisplayName("다른 공개 GET /exhibitions 경로는 여전히 비로그인 접근이 된다")
    void otherExhibitionGetsStayPublic() throws Exception {
        mvc.perform(get("/api/exhibitions").contextPath("/api"))
                .andExpect(status().isOk());
    }


    @Test
    @DisplayName("같은 항목을 필수이면서 숨김으로 저장할 수 없다 (400)")
    void rejectsRequiredAndHiddenTogether() throws Exception {
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(false, true, false, false, true, false, false, false, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_015"));
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(false, false, true, false, false, true, false, false, false)))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(false, false, false, true, false, false, true, false, false)))
                .andExpect(status().isBadRequest());

        assertTrue(fieldSettings.findByExhibitionId(exhibition.getId()).isEmpty());
    }

    @Test
    @DisplayName("필수가 아닌 항목은 숨길 수 있고, 대표 이미지 필수와 다른 항목 숨김은 함께 쓸 수 있다")
    void allowsHiddenWhenNotRequired() throws Exception {
        mvc.perform(withToken(put(exhibition.getId()), ownerToken)
                        .content(settingsJson(true, true, false, false, false, true, true, true, true)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("boolean 이 아닌 값(문자열 \"true\", 숫자 1, null)은 400")
    void rejectsNonBooleanValues() throws Exception {
        String stringTrue = ALL_FALSE.replaceFirst("\"mainImg\":false", "\"mainImg\":\"true\"");
        String numberOne = ALL_FALSE.replaceFirst("\"mainImg\":false", "\"mainImg\":1");
        String nullValue = ALL_FALSE.replaceFirst("\"mainImg\":false", "\"mainImg\":null");

        mvc.perform(withToken(put(exhibition.getId()), ownerToken).content(stringTrue))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(put(exhibition.getId()), ownerToken).content(numberOne))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(put(exhibition.getId()), ownerToken).content(nullValue))
                .andExpect(status().isBadRequest());

        assertTrue(fieldSettings.findByExhibitionId(exhibition.getId()).isEmpty());
    }

    private static String settingsJson(boolean rMain, boolean rSize, boolean rMat, boolean rMap,
                                       boolean hSize, boolean hMat, boolean hMap, boolean hPeriod, boolean hYear) {
        return "{\"required\":{\"mainImg\":" + rMain + ",\"size\":" + rSize + ",\"materials\":" + rMat
                + ",\"locationMap\":" + rMap + "},\"hidden\":{\"size\":" + hSize + ",\"materials\":" + hMat
                + ",\"locationMap\":" + hMap + ",\"productionPeriod\":" + hPeriod + ",\"productionYear\":" + hYear + "}}";
    }

    private Account account(Role role) {
        return accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private Exhibition exhibition(Account owner) {
        return exhibitions.saveAndFlush(Exhibition.builder().account(owner).univName("대학")
                .deptName("학과").slug("field-" + UUID.randomUUID()).build());
    }

    private Artwork artwork(Exhibition exhibition, ArtworkStatus status, String mainImg) {
        Artwork artwork = Artwork.builder().title("작품").description("설명").exhibition(exhibition)
                .mainImg(mainImg).width(BigDecimal.ONE).height(BigDecimal.ONE).build();
        artwork.changeStatus(status);
        artwork.replaceMaterials(List.of("재료"));
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private ArtworkStatus statusOf(Artwork artwork) {
        return artworks.findById(artwork.getId()).orElseThrow().getStatus();
    }

    private MockHttpServletRequestBuilder put(UUID exhibitionId) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/exhibitions/{id}/field-settings", exhibitionId).contentType(MediaType.APPLICATION_JSON);
    }

    private <T extends MockHttpServletRequestBuilder> T withToken(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }

    private void endRequest() {
        em.flush();
        em.clear();
    }
}
