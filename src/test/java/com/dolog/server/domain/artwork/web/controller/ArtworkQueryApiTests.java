package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.like.entity.ArtworkLike;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkQueryApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired ExhibitionArtistMapRepository participations;
    @Autowired ExhibitionFieldSettingsRepository fieldSettings;
    @Autowired ArtworkLikeRepository likes;
    @Autowired EntityManager em;

    private Artist me;
    private ArtistProfile myProfile;
    private Exhibition exhibition;
    private ExhibitionZone zone;

    @BeforeEach
    void setUp() {
        me = artist("나작가");
        exhibition = exhibition();
        zone = zone(exhibition, "1구역", 1);
        myProfile = join(me, exhibition, ExhibitionArtistStatus.JOINED);
    }

    // ---------------- 전시 작품 목록 (C-02) ----------------

    @Test
    @DisplayName("전시 작품 목록에는 공개이면서 작품, 구역 모두 숨기지 않은 작품만 나온다")
    void exhibitionListShowsOnlyVisibleArtworks() throws Exception {
        ExhibitionZone hiddenZone = zone(exhibition, "숨긴 구역", 2);
        hiddenZone.changeHidden(true);

        Artwork visible = submitted(artworkOf(me, "공개 작품"), zone, ArtworkStatus.PUBLISHED);
        Artwork draft = submitted(artworkOf(me, "비공개 작품"), zone, ArtworkStatus.DRAFT);
        Artwork hidden = submitted(artworkOf(me, "숨긴 작품"), zone, ArtworkStatus.PUBLISHED);
        hidden.hide(LocalDateTime.now());
        Artwork inHiddenZone = submitted(artworkOf(me, "숨긴 구역 작품"), hiddenZone, ArtworkStatus.PUBLISHED);
        endRequest();

        mvc.perform(api(get("/api/exhibitions/{id}/artworks", exhibition.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zones", hasSize(1)))
                .andExpect(jsonPath("$.data.zones[0].zoneName").value("1구역"))
                .andExpect(jsonPath("$.data.zones[*].categories[*].artworks[*].id",
                        contains(visible.getId().toString())));

        // 검색도 같은 조건
        mvc.perform(api(get("/api/exhibitions/{id}/artworks", exhibition.getId()).param("search", "작품")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zones[*].categories[*].artworks[*].id",
                        contains(visible.getId().toString())));
    }

    // ---------------- 작품 상세 (C-03, C-04) ----------------

    @Test
    @DisplayName("전시 URL 상세는 비공개, 숨김, 숨긴 구역 작품이면 404, 두록 URL 상세는 숨김이어도 공개면 200")
    void detailVisibilityByUrl() throws Exception {
        Artwork draft = submitted(artworkOf(me, "비공개"), zone, ArtworkStatus.DRAFT);
        Artwork hidden = submitted(artworkOf(me, "숨김"), zone, ArtworkStatus.PUBLISHED);
        hidden.hide(LocalDateTime.now());
        ExhibitionZone hiddenZone = zone(exhibition, "숨긴 구역", 2);
        hiddenZone.changeHidden(true);
        Artwork inHiddenZone = submitted(artworkOf(me, "숨긴 구역"), hiddenZone, ArtworkStatus.PUBLISHED);
        endRequest();

        for (Artwork artwork : List.of(draft, hidden, inHiddenZone)) {
            mvc.perform(api(exhibitionDetail(artwork)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        }

        mvc.perform(api(dologDetail(draft))).andExpect(status().isNotFound());
        mvc.perform(api(dologDetail(hidden))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("숨김"));
        mvc.perform(api(dologDetail(inHiddenZone))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("다른 전시 ID 로 전시 URL 상세를 부르면 404")
    void exhibitionDetailOfOtherExhibition() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"), zone, ArtworkStatus.PUBLISHED);
        Exhibition other = exhibition();
        endRequest();

        mvc.perform(api(get("/api/exhibitions/{e}/artworks/{a}", other.getId(), artwork.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("두록 URL 상세는 출품하지 않은 공개 작품도 보여주고, 삭제한 작품은 404")
    void dologDetailOfUnsubmittedAndDeleted() throws Exception {
        Artwork unsubmitted = artworkOf(me, "개인 작품");
        unsubmitted.changeStatus(ArtworkStatus.PUBLISHED);
        Artwork deleted = submitted(artworkOf(me, "지운 작품"), zone, ArtworkStatus.PUBLISHED);
        deleted.markDeleted(LocalDateTime.now());
        endRequest();

        mvc.perform(api(dologDetail(unsubmitted)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].nameKo").value("나작가"))
                .andExpect(jsonPath("$.data.sameCategoryArtworks", hasSize(0)))
                .andExpect(jsonPath("$.data.alphabeticalArtworks", hasSize(0)));

        mvc.perform(api(dologDetail(deleted))).andExpect(status().isNotFound());
        mvc.perform(api(exhibitionDetail(deleted))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상세 응답에 V2 필드가 담기고, 조회할 때마다 조회수가 1씩 오른다")
    void detailHasV2FieldsAndCountsViews() throws Exception {
        Artwork artwork = Artwork.builder().title("파도의 그릇").category("도자").description("설명")
                .shortIntro("물결 그릇").width(new BigDecimal("32.0")).height(new BigDecimal("30.0"))
                .depth(new BigDecimal("7.0")).productionStartYear(2026).productionEndYear(2026)
                .productionEndMonth(6).productionEndDay(15).purchaseUrl("https://shop")
                .purchaseChatUrl("https://chat").showPurchaseButton(true)
                .locationMap("https://s3/map.webp").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(me).artistRole("도예").build());
        artwork.replaceMaterials(List.of("백자토", "청화안료"));
        artworks.saveAndFlush(artwork);
        submitted(artwork, zone, ArtworkStatus.PUBLISHED);
        endRequest();

        mvc.perform(api(exhibitionDetail(artwork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortIntro").value("물결 그릇"))
                .andExpect(jsonPath("$.data.materials", contains("백자토", "청화안료")))
                .andExpect(jsonPath("$.data.width").value(32.0))
                .andExpect(jsonPath("$.data.depth").value(7.0))
                .andExpect(jsonPath("$.data.productionStartYear").value(2026))
                .andExpect(jsonPath("$.data.productionEndMonth").value(6))
                .andExpect(jsonPath("$.data.purchaseChatUrl").value("https://chat"))
                .andExpect(jsonPath("$.data.showPurchaseButton").value(true))
                .andExpect(jsonPath("$.data.locationMap").value("https://s3/map.webp"))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.participants[0].role").value("도예"))
                .andExpect(jsonPath("$.data.participants[0].profileId").value(myProfile.getId().toString()))
                .andExpect(jsonPath("$.data.material").doesNotExist())
                .andExpect(jsonPath("$.data.size").doesNotExist());
        endRequest();

        mvc.perform(api(dologDetail(artwork)))
                .andExpect(jsonPath("$.data.viewCount").value(2));
        endRequest();

        assertEquals(2L, artworks.findById(artwork.getId()).orElseThrow().getViewCount());
    }

    @Test
    @DisplayName("없는 작품, 비공개 작품을 조회하면 조회수가 오르지 않는다")
    void notFoundDoesNotCountView() throws Exception {
        Artwork draft = submitted(artworkOf(me, "비공개"), zone, ArtworkStatus.DRAFT);
        endRequest();

        mvc.perform(api(dologDetail(draft))).andExpect(status().isNotFound());
        endRequest();

        assertEquals(0L, artworks.findById(draft.getId()).orElseThrow().getViewCount());
    }

    @Test
    @DisplayName("좋아요 수와 이 방문자의 좋아요 여부가 담기고, 방문자 ID 형식이 틀려도 상세는 열린다")
    void detailHasLikeCountAndLiked() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"), zone, ArtworkStatus.PUBLISHED);
        likes.saveAndFlush(ArtworkLike.builder().artwork(artwork).visitorId("vis_me").build());
        likes.saveAndFlush(ArtworkLike.builder().artwork(artwork).visitorId("vis_other").build());
        endRequest();

        mvc.perform(api(exhibitionDetail(artwork)).header("X-Visitor-Id", "vis_me"))
                .andExpect(jsonPath("$.data.likeCount").value(2))
                .andExpect(jsonPath("$.data.liked").value(true));
        mvc.perform(api(dologDetail(artwork)).param("visitorId", "vis_stranger"))
                .andExpect(jsonPath("$.data.liked").value(false));
        mvc.perform(api(dologDetail(artwork)))
                .andExpect(jsonPath("$.data.liked").value(false));
        mvc.perform(api(dologDetail(artwork)).header("X-Visitor-Id", "잘못된 값!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    @DisplayName("전시 항목 숨김 설정: 사이즈, 재료, 위치 지도는 비우고 제작 기간을 숨기면 연도만 남긴다")
    void appliesHiddenFieldSettings() throws Exception {
        Artwork artwork = fullArtwork();
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.defaultsFor(exhibition);
        settings.update(false, false, false, false, true, true, true, true, false);
        fieldSettings.saveAndFlush(settings);
        endRequest();

        mvc.perform(api(exhibitionDetail(artwork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.width").value(nullValue()))
                .andExpect(jsonPath("$.data.height").value(nullValue()))
                .andExpect(jsonPath("$.data.depth").value(nullValue()))
                .andExpect(jsonPath("$.data.materials", hasSize(0)))
                .andExpect(jsonPath("$.data.locationMap").value(nullValue()))
                .andExpect(jsonPath("$.data.productionStartYear").value(2025))
                .andExpect(jsonPath("$.data.productionEndYear").value(2026))
                .andExpect(jsonPath("$.data.productionStartMonth").value(nullValue()))
                .andExpect(jsonPath("$.data.productionEndDay").value(nullValue()))
                .andExpect(jsonPath("$.data.title").value("모든 항목 작품"));
    }

    @Test
    @DisplayName("제작 연도를 숨기면 제작 기간 전체를 비운다")
    void hidesWholeProductionPeriodWhenYearHidden() throws Exception {
        Artwork artwork = fullArtwork();
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.defaultsFor(exhibition);
        settings.update(false, false, false, false, false, false, false, false, true);
        fieldSettings.saveAndFlush(settings);
        endRequest();

        mvc.perform(api(exhibitionDetail(artwork)))
                .andExpect(jsonPath("$.data.productionStartYear").value(nullValue()))
                .andExpect(jsonPath("$.data.productionEndYear").value(nullValue()))
                .andExpect(jsonPath("$.data.productionEndMonth").value(nullValue()))
                .andExpect(jsonPath("$.data.width").value(32.0))
                .andExpect(jsonPath("$.data.materials", hasSize(1)));
    }

    @Test
    @DisplayName("작품 정보 숨기기는 전시 웹사이트에만 적용하고 두록 URL 상세는 모두 보여준다")
    void hiddenFieldSettingsDoNotApplyToDologUrl() throws Exception {
        Artwork artwork = fullArtwork();
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.defaultsFor(exhibition);
        settings.update(false, false, false, false, true, true, true, false, true);
        fieldSettings.saveAndFlush(settings);
        endRequest();

        mvc.perform(api(dologDetail(artwork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.width").value(32.0))
                .andExpect(jsonPath("$.data.materials", contains("유채")))
                .andExpect(jsonPath("$.data.locationMap").value("https://s3/map.webp"))
                .andExpect(jsonPath("$.data.productionStartYear").value(2025))
                .andExpect(jsonPath("$.data.productionEndDay").value(15));
    }

    @Test
    @DisplayName("전시에서 제외된 공동 작가는 이름과 역할만 남고 프로필 정보는 비운다 (#352)")
    void removedCoArtistShowsCreditOnly() throws Exception {
        Artist removed = artist("제외작가");
        ArtistProfile removedProfile = join(removed, exhibition, ExhibitionArtistStatus.JOINED);
        Artwork artwork = submitted(artworkOf(me, "공동 작품"), zone, ArtworkStatus.PUBLISHED);
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(removed)
                .artistProfile(removedProfile).artistRole("공동").build());
        artworks.saveAndFlush(artwork);
        participations.findByExhibitionIdAndArtistId(exhibition.getId(), removed.getId()).orElseThrow()
                .updateStatus(ExhibitionArtistStatus.REMOVED);
        endRequest();

        mvc.perform(api(exhibitionDetail(artwork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '나작가')].profileId",
                        contains(myProfile.getId().toString())))
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '나작가')].email",
                        contains("me@test.com")))
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '제외작가')].role", contains("공동")))
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '제외작가')].profileId",
                        contains(nullValue())))
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '제외작가')].email",
                        contains(nullValue())))
                .andExpect(jsonPath("$.data.participants[?(@.nameKo == '제외작가')].bio",
                        contains(nullValue())));
    }

    @Test
    @DisplayName("이전/다음, 연관 작품도 URL 의 노출 조건을 따른다")
    void relatedAndNavigationFollowVisibility() throws Exception {
        Artwork prevHidden = submitted(artworkOf(me, "앞 숨김"), zone, ArtworkStatus.PUBLISHED, 10);
        prevHidden.hide(LocalDateTime.now());
        Artwork current = submitted(artworkOf(me, "가운데"), zone, ArtworkStatus.PUBLISHED, 20);
        Artwork nextDraft = submitted(artworkOf(me, "뒤 비공개"), zone, ArtworkStatus.DRAFT, 30);
        endRequest();

        mvc.perform(api(exhibitionDetail(current)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alphabeticalArtworks", hasSize(0)))
                .andExpect(jsonPath("$.data.sameCategoryArtworks", hasSize(0)));

        // 두록 URL 은 숨김은 무시하고 비공개만 뺀다
        mvc.perform(api(dologDetail(current)))
                .andExpect(jsonPath("$.data.alphabeticalArtworks[*].id", contains(prevHidden.getId().toString())))
                .andExpect(jsonPath("$.data.alphabeticalArtworks[0].type").value("prev"))
                .andExpect(jsonPath("$.data.sameCategoryArtworks[*].id", contains(prevHidden.getId().toString())));
    }

    // ---------------- 작품 전체 목록 (C-01) ----------------

    @Test
    @DisplayName("작품 전체 목록과 메인 랜덤 목록에는 공개 작품만 나온다 (숨김은 상관없음)")
    void allArtworksShowsOnlyPublished() throws Exception {
        String keyword = "전체목록-" + UUID.randomUUID();
        Artwork published = submitted(artworkOf(me, keyword + "-공개"), zone, ArtworkStatus.PUBLISHED);
        Artwork hidden = submitted(artworkOf(me, keyword + "-숨김"), zone, ArtworkStatus.PUBLISHED);
        hidden.hide(LocalDateTime.now());
        submitted(artworkOf(me, keyword + "-비공개"), zone, ArtworkStatus.DRAFT);
        endRequest();

        mvc.perform(api(get("/api/artworks").param("search", keyword).param("sort", "title")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id",
                        containsInAnyOrder(published.getId().toString(), hidden.getId().toString())));

        mvc.perform(api(get("/api/artworks").param("main", "true").param("search", keyword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id",
                        containsInAnyOrder(published.getId().toString(), hidden.getId().toString())));
    }

    // ---------------- helpers ----------------

    private Artwork fullArtwork() {
        Artwork artwork = Artwork.builder().title("모든 항목 작품").category("회화").description("설명")
                .width(new BigDecimal("32.0")).height(new BigDecimal("30.0")).depth(new BigDecimal("5.0"))
                .productionStartYear(2025).productionStartMonth(3).productionStartDay(1)
                .productionEndYear(2026).productionEndMonth(6).productionEndDay(15)
                .locationMap("https://s3/map.webp").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(me).artistRole("").build());
        artwork.replaceMaterials(List.of("유채"));
        artworks.saveAndFlush(artwork);
        return submitted(artwork, zone, ArtworkStatus.PUBLISHED);
    }

    private Account account(Role role) {
        return accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
    }

    private Artist artist(String name) {
        return artists.saveAndFlush(Artist.builder().account(account(Role.ARTIST_ADMIN)).nameKo(name).build());
    }

    private Exhibition exhibition() {
        return exhibitions.saveAndFlush(Exhibition.builder().account(account(Role.EXHIBITION_ADMIN))
                .univName("테스트 대학").deptName("테스트 학과").slug("query-" + UUID.randomUUID()).build());
    }

    private ExhibitionZone zone(Exhibition exhibition, String name, int orderId) {
        return zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name(name).orderId(orderId).build());
    }

    private ArtistProfile join(Artist artist, Exhibition exhibition, ExhibitionArtistStatus status) {
        participations.saveAndFlush(ExhibitionArtistMap.builder()
                .exhibition(exhibition).artist(artist).status(status).build());
        String email = artist == me ? "me@test.com" : "other@test.com";
        return profiles.saveAndFlush(ArtistProfile.builder().artist(artist).exhibition(exhibition)
                .nameKo(artist.getNameKo()).email(email).bio("소개").build());
    }

    private Artwork artworkOf(Artist artist, String title) {
        Artwork artwork = Artwork.builder().title(title).category("회화").description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private Artwork submitted(Artwork artwork, ExhibitionZone zone, ArtworkStatus status) {
        return submitted(artwork, zone, status, 10);
    }

    private Artwork submitted(Artwork artwork, ExhibitionZone zone, ArtworkStatus status, int orderIndex) {
        artwork.submitTo(exhibition, zone);
        artwork.getArtworkArtistMaps().stream()
                .filter(map -> map.getArtist().getId().equals(me.getId()))
                .forEach(map -> map.linkProfile(myProfile));
        artwork.updateOrder(orderIndex);
        artwork.changeStatus(status);
        return artworks.saveAndFlush(artwork);
    }

    private MockHttpServletRequestBuilder exhibitionDetail(Artwork artwork) {
        return get("/api/exhibitions/{e}/artworks/{a}", exhibition.getId(), artwork.getId());
    }

    private MockHttpServletRequestBuilder dologDetail(Artwork artwork) {
        return get("/api/artworks/{a}", artwork.getId());
    }

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/api");
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
