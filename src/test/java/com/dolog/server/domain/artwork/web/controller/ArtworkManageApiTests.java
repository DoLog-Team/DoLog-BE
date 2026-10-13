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
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkPlanLimitService;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.notification.entity.Notification;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.repository.NotificationRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkManageApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired ExhibitionFieldSettingsRepository fieldSettings;
    @Autowired NotificationRepository notifications;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @MockitoSpyBean ArtworkPlanLimitService artworkPlanLimitService;

    private Artist me;
    private Exhibition exhibition;
    private String ownerToken;
    private ExhibitionZone zoneA;
    private ExhibitionZone zoneB;

    @BeforeEach
    void setUp() {
        me = artist("김작가");
        Account owner = account(Role.EXHIBITION_ADMIN);
        ownerToken = token(owner);
        exhibition = exhibition(owner);
        zoneA = zone(exhibition, "A 구역", 1);
        zoneB = zone(exhibition, "B 구역", 2);
    }

    @Test
    @DisplayName("전체 작품 관리는 숨기지 않은 출품작을 공개 여부와 관계없이 그룹 순서, 그룹 안 순서로 주고 그룹 미지정은 맨 뒤다")
    void listsNotHiddenArtworksInExhibitionOrder() throws Exception {
        Artwork unassigned = submitted(artworkOf(me, "미지정 작품"), null, 10);
        Artwork b1 = submitted(artworkOf(me, "B 첫 작품"), zoneB, 10);
        Artwork a2 = submitted(artworkOf(me, "A 두번째"), zoneA, 20);
        Artwork a1 = published(submitted(artworkOf(me, "A 첫 작품"), zoneA, 10));
        Artwork hidden = submitted(artworkOf(me, "숨긴 작품"), zoneA, 30);
        hidden.hide(LocalDateTime.now());
        artworkOf(me, "출품 안 한 작품");
        Exhibition other = exhibition(account(Role.EXHIBITION_ADMIN));
        Artwork othersArtwork = artworkOf(me, "다른 전시 작품");
        othersArtwork.submitTo(other, zone(other, "다른 구역", 1));
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(
                        a1.getId().toString(), a2.getId().toString(), b1.getId().toString(), unassigned.getId().toString())))
                .andExpect(jsonPath("$.data.artworks[0].title").value("A 첫 작품"))
                .andExpect(jsonPath("$.data.artworks[0].artistNames[0]").value("김작가"))
                .andExpect(jsonPath("$.data.artworks[0].zoneId").value(zoneA.getId().toString()))
                .andExpect(jsonPath("$.data.artworks[0].zoneName").value("A 구역"))
                .andExpect(jsonPath("$.data.artworks[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.artworks[0].visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.artworks[0].hidden").value(false))
                .andExpect(jsonPath("$.data.artworks[0].planLimitExceeded").value(false))
                .andExpect(jsonPath("$.data.artworks[0].completionRate").value(100))
                .andExpect(jsonPath("$.data.artworks[3].zoneId").value(nullValue()))
                .andExpect(jsonPath("$.data.artworks[3].zoneName").value(nullValue()))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.planLimitExceededCount").value(0));
    }

    @Test
    @DisplayName("숨긴 작품 관리는 숨긴 작품만 준다")
    void listsHiddenArtworks() throws Exception {
        submitted(artworkOf(me, "보이는 작품"), zoneA, 10);
        Artwork hidden = submitted(artworkOf(me, "숨긴 작품"), zoneA, 20);
        hidden.hide(LocalDateTime.now());
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(hidden.getId().toString())))
                .andExpect(jsonPath("$.data.artworks[0].hidden").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));
    }

    @Test
    @DisplayName("공개 여부는 공개 / 비공개(진행도 100%) / 임시저장(100% 미만)으로 나눠 필터한다")
    void filtersByVisibility() throws Exception {
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder().exhibition(exhibition).requiredMainImg(true).build());
        Artwork publicArtwork = published(withMainImg(submitted(artworkOf(me, "공개"), zoneA, 10)));
        Artwork privateArtwork = withMainImg(submitted(artworkOf(me, "비공개"), zoneA, 20));
        Artwork draftArtwork = submitted(artworkOf(me, "임시저장"), zoneA, 30);
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "PUBLIC"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(publicArtwork.getId().toString())));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "PRIVATE"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(privateArtwork.getId().toString())))
                .andExpect(jsonPath("$.data.artworks[0].visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.artworks[0].completionRate").value(100));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "DRAFT"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(draftArtwork.getId().toString())))
                .andExpect(jsonPath("$.data.artworks[0].visibility").value("DRAFT"))
                .andExpect(jsonPath("$.data.artworks[0].completionRate").value(0));
    }

    @Test
    @DisplayName("필수 항목이 없는 전시는 임시저장 작품이 없고 비공개 작품은 모두 비공개다")
    void noRequiredFieldsMeansNoDraft() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"), zoneA, 10);
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "DRAFT"))
                .andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "PRIVATE"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(artwork.getId().toString())));
    }

    @Test
    @DisplayName("그룹, 그룹 미지정으로 필터하고 둘을 같이 쓰면 400")
    void filtersByZone() throws Exception {
        Artwork inA = submitted(artworkOf(me, "A 작품"), zoneA, 10);
        Artwork unassigned = submitted(artworkOf(me, "미지정 작품"), null, 10);
        submitted(artworkOf(me, "B 작품"), zoneB, 10);
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("zoneId", zoneA.getId().toString()))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(inA.getId().toString())));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("unassigned", "true"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(unassigned.getId().toString())));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false")
                        .param("zoneId", zoneA.getId().toString()).param("unassigned", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_021"));
    }

    @Test
    @DisplayName("다른 전시의 그룹으로 필터하면 400")
    void rejectsZoneOfOtherExhibition() throws Exception {
        ExhibitionZone otherZone = zone(exhibition(account(Role.EXHIBITION_ADMIN)), "다른 구역", 1);

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("zoneId", otherZone.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_012"));
    }

    @Test
    @DisplayName("작품명과 작가 이름으로 검색한다")
    void searchesByTitleAndArtistName() throws Exception {
        Artist other = artist("박두록");
        Artwork byTitle = submitted(artworkOf(me, "봄의 기록"), zoneA, 10);
        Artwork byArtist = submitted(artworkOf(other, "여름"), zoneA, 20);
        submitted(artworkOf(me, "겨울"), zoneA, 30);
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("search", "봄"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(byTitle.getId().toString())));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("search", "박두"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(byArtist.getId().toString())));
    }

    @Test
    @DisplayName("페이지 단위로 자르고 전체 수와 페이지 수를 준다. 범위를 벗어나면 400")
    void pagesResults() throws Exception {
        submitted(artworkOf(me, "1"), zoneA, 10);
        submitted(artworkOf(me, "2"), zoneA, 20);
        Artwork third = submitted(artworkOf(me, "3"), zoneA, 30);
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(third.getId().toString())))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_022"));
    }

    @Test
    @DisplayName("플랜 한도 초과 작품은 표시하고 배너용 수는 필터와 관계없이 숨기지 않은 초과 작품만 센다")
    void countsPlanLimitExceeded() throws Exception {
        Artwork exceeded = submitted(artworkOf(me, "초과"), zoneA, 10);
        exceeded.markPlanLimitExceeded(LocalDateTime.now());
        Artwork exceededButHidden = submitted(artworkOf(me, "초과 후 숨김"), zoneA, 20);
        exceededButHidden.markPlanLimitExceeded(LocalDateTime.now());
        exceededButHidden.hide(LocalDateTime.now());
        endRequest();

        mvc.perform(withToken(list(), ownerToken).param("hidden", "false"))
                .andExpect(jsonPath("$.data.artworks[0].planLimitExceeded").value(true))
                .andExpect(jsonPath("$.data.planLimitExceededCount").value(1));
        mvc.perform(withToken(list(), ownerToken).param("hidden", "true"))
                .andExpect(jsonPath("$.data.artworks[0].planLimitExceeded").value(false))
                .andExpect(jsonPath("$.data.planLimitExceededCount").value(1));
    }

    @Test
    @DisplayName("작품 관리 목록은 본인 전시의 전시 어드민만 볼 수 있다")
    void listRequiresOwner() throws Exception {
        String otherOwnerToken = otherOwnerToken();

        mvc.perform(withToken(list(), otherOwnerToken).param("hidden", "false"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_014"));
        mvc.perform(withToken(list(), token(me.getAccount())).param("hidden", "false"))
                .andExpect(status().isForbidden());
        mvc.perform(list().contextPath("/api").param("hidden", "false"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("여러 작품을 한 번에 숨기고, 이미 숨긴 작품은 처음 시각을 유지하며 바뀐 작품만 알린다")
    void hidesArtworksInBulk() throws Exception {
        Artwork first = submitted(artworkOf(me, "첫 작품"), zoneA, 10);
        Artwork second = submitted(artworkOf(me, "둘째 작품"), zoneA, 20);
        Artwork alreadyHidden = submitted(artworkOf(me, "이미 숨긴 작품"), zoneA, 30);
        LocalDateTime hiddenAt = LocalDateTime.of(2026, 10, 1, 9, 0);
        alreadyHidden.hide(hiddenAt);
        endRequest();

        mvc.perform(withToken(bulkHidden(true, first, second, alreadyHidden), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true))
                .andExpect(jsonPath("$.data.changedCount").value(2))
                .andExpect(jsonPath("$.data.artworks[*].artworkId").value(contains(
                        first.getId().toString(), second.getId().toString(), alreadyHidden.getId().toString())))
                .andExpect(jsonPath("$.data.artworks[2].hiddenAt").value("2026-10-01 09:00:00"));

        endRequest();
        assertTrue(artworks.findById(first.getId()).orElseThrow().isHidden());
        assertTrue(artworks.findById(second.getId()).orElseThrow().isHidden());
        assertEquals(hiddenAt, artworks.findById(alreadyHidden.getId()).orElseThrow().getHiddenAt());
        assertEquals(ArtworkStatus.DRAFT, artworks.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(2, notificationsOf(me, NotificationType.ARTWORK_HIDDEN).size());
    }

    @Test
    @DisplayName("여러 작품을 한 번에 다시 보이게 한다")
    void showsArtworksInBulk() throws Exception {
        Artwork first = submitted(artworkOf(me, "첫 작품"), zoneA, 10);
        Artwork second = submitted(artworkOf(me, "둘째 작품"), zoneA, 20);
        first.hide(LocalDateTime.now());
        second.hide(LocalDateTime.now());
        endRequest();

        mvc.perform(withToken(bulkHidden(false, first, second), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changedCount").value(2))
                .andExpect(jsonPath("$.data.artworks[0].hidden").value(false))
                .andExpect(jsonPath("$.data.artworks[0].hiddenAt").value(nullValue()));

        endRequest();
        assertFalse(artworks.findById(first.getId()).orElseThrow().isHidden());
        assertEquals(2, notificationsOf(me, NotificationType.ARTWORK_SHOWN).size());
    }

    @Test
    @DisplayName("하나라도 이 전시 출품작이 아니면 400 이고 아무것도 숨기지 않는다")
    void bulkHiddenRejectsArtworkOutsideExhibition() throws Exception {
        Artwork mine = submitted(artworkOf(me, "내 전시 작품"), zoneA, 10);
        Artwork notSubmitted = artworkOf(me, "출품 안 한 작품");
        endRequest();

        mvc.perform(withToken(bulkHidden(true, mine, notSubmitted), ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_020"));

        endRequest();
        assertFalse(artworks.findById(mine.getId()).orElseThrow().isHidden());
    }

    @Test
    @DisplayName("작품 숨김 일괄 처리는 본인 전시의 전시 어드민만, 빈 목록은 400")
    void bulkHiddenRequiresOwnerAndArtworks() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"), zoneA, 10);
        endRequest();

        mvc.perform(withToken(bulkHidden(true, artwork), otherOwnerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_014"));
        mvc.perform(withToken(bulkHidden(true), ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("여러 작품의 그룹을 바꾸면 새 그룹 맨 뒤 순서가 되고, 이미 그 그룹인 작품은 건너뛰며 바뀐 작품만 알린다")
    void assignsZoneInBulk() throws Exception {
        Artwork inB = submitted(artworkOf(me, "B 기존 작품"), zoneB, 30);
        Artwork fromA = submitted(artworkOf(me, "A 작품"), zoneA, 10);
        Artwork fromNone = submitted(artworkOf(me, "미지정 작품"), null, 10);
        endRequest();

        mvc.perform(withToken(bulkZone(zoneB.getId(), fromA, fromNone, inB), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zoneId").value(zoneB.getId().toString()))
                .andExpect(jsonPath("$.data.zoneName").value("B 구역"))
                .andExpect(jsonPath("$.data.changedCount").value(2))
                .andExpect(jsonPath("$.data.artworkIds").value(contains(
                        fromA.getId().toString(), fromNone.getId().toString(), inB.getId().toString())));

        endRequest();
        Artwork movedA = artworks.findById(fromA.getId()).orElseThrow();
        Artwork movedNone = artworks.findById(fromNone.getId()).orElseThrow();
        assertEquals(zoneB.getId(), movedA.getExhibitionZone().getId());
        assertEquals(zoneB.getId(), movedNone.getExhibitionZone().getId());
        assertEquals(30, artworks.findById(inB.getId()).orElseThrow().getOrderIndex());
        assertTrue(movedA.getOrderIndex() > 30);
        assertTrue(movedNone.getOrderIndex() > movedA.getOrderIndex());
        assertEquals(2, notificationsOf(me, NotificationType.ARTWORK_GROUP_ASSIGNED).size());
    }

    @Test
    @DisplayName("zoneId 를 빼거나 null 로 보내면 400 이고 그룹이 해제되지 않는다 (그룹 해제는 지원하지 않음)")
    void bulkZoneRequiresZoneId() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"), zoneA, 20);
        endRequest();

        mvc.perform(withToken(patch("/api/exhibitions/{id}/artworks/zone", exhibition.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artworkIds\":[\"" + artwork.getId() + "\"],\"zoneId\":null}"), ownerToken))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(patch("/api/exhibitions/{id}/artworks/zone", exhibition.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artworkIds\":[\"" + artwork.getId() + "\"]}"), ownerToken))
                .andExpect(status().isBadRequest());

        endRequest();
        assertEquals(zoneA.getId(), artworks.findById(artwork.getId()).orElseThrow().getExhibitionZone().getId());
    }

    @Test
    @DisplayName("그룹이 바뀐 작품이 있을 때만 플랜 한도를 다시 계산한다 (순서가 바뀌므로)")
    void bulkZoneRecomputesPlanLimitOnlyWhenChanged() throws Exception {
        Artwork inB = submitted(artworkOf(me, "B 작품"), zoneB, 10);
        Artwork inA = submitted(artworkOf(me, "A 작품"), zoneA, 10);
        endRequest();

        mvc.perform(withToken(bulkZone(zoneB.getId(), inB), ownerToken)).andExpect(status().isOk());
        verify(artworkPlanLimitService, never()).recompute(exhibition.getId());

        mvc.perform(withToken(bulkZone(zoneB.getId(), inA), ownerToken)).andExpect(status().isOk());
        verify(artworkPlanLimitService).recompute(exhibition.getId());
    }

    @Test
    @DisplayName("공개 여부 필터와 응답의 visibility, completionRate 는 필수 항목 조합이 무엇이든 같은 판정을 한다")
    void visibilityFilterMatchesCompletionRate() throws Exception {
        // 대표 이미지, 사이즈, 재료, 위치 지도를 채운 조합 16가지
        for (int filled = 0; filled < 16; filled++) {
            Artwork artwork = submitted(artworkOf(me, "작품 " + filled), zoneA, 10 + filled);
            if ((filled & 1) != 0) artwork.updateMainImg("https://s3/main.webp");
            if ((filled & 2) != 0) artwork.updateSize(BigDecimal.ONE, BigDecimal.ONE, null);
            if ((filled & 4) != 0) artwork.replaceMaterials(List.of("유화"));
            if ((filled & 8) != 0) artwork.updateLocationMap("https://s3/map.webp");
        }
        // 경계: 가로만 있는 사이즈, 빈 문자열 이미지
        submitted(artworkOf(me, "가로만"), zoneA, 50).updateSize(BigDecimal.ONE, null, null);
        submitted(artworkOf(me, "빈 이미지"), zoneA, 60).updateMainImg("");
        int total = 18;
        ExhibitionFieldSettings settings = fieldSettings.saveAndFlush(ExhibitionFieldSettings.defaultsFor(exhibition));
        endRequest();

        for (int required = 0; required < 16; required++) {
            ExhibitionFieldSettings found = fieldSettings.findById(settings.getId()).orElseThrow();
            found.update((required & 1) != 0, (required & 2) != 0, (required & 4) != 0, (required & 8) != 0,
                    false, false, false, false, false);
            endRequest();

            ExhibitionFieldSettings current = fieldSettings.findById(settings.getId()).orElseThrow();
            int expectedPrivate = (int) artworks.findByExhibitionIdAndStatus(exhibition.getId(), ArtworkStatus.DRAFT)
                    .stream().filter(current::isSatisfiedBy).count();

            mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "PRIVATE").param("size", "100"))
                    .andExpect(jsonPath("$.data.totalCount").value(expectedPrivate))
                    .andExpect(jsonPath("$.data.artworks[?(@.visibility != 'PRIVATE')]").isEmpty())
                    .andExpect(jsonPath("$.data.artworks[?(@.completionRate != 100)]").isEmpty());
            mvc.perform(withToken(list(), ownerToken).param("hidden", "false").param("visibility", "DRAFT").param("size", "100"))
                    .andExpect(jsonPath("$.data.totalCount").value(total - expectedPrivate))
                    .andExpect(jsonPath("$.data.artworks[?(@.visibility != 'DRAFT')]").isEmpty())
                    .andExpect(jsonPath("$.data.artworks[?(@.completionRate == 100)]").isEmpty());
        }
    }

    @Test
    @DisplayName("다른 전시의 그룹이나 이 전시 출품작이 아닌 작품이 있으면 400 이고 아무것도 바꾸지 않는다")
    void bulkZoneRejectsInvalidTargets() throws Exception {
        Artwork mine = submitted(artworkOf(me, "작품"), zoneA, 10);
        ExhibitionZone otherZone = zone(exhibition(account(Role.EXHIBITION_ADMIN)), "다른 구역", 1);
        Artwork notSubmitted = artworkOf(me, "출품 안 한 작품");
        endRequest();

        mvc.perform(withToken(bulkZone(otherZone.getId(), mine), ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_012"));
        mvc.perform(withToken(bulkZone(zoneB.getId(), mine, notSubmitted), ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_020"));
        mvc.perform(withToken(bulkZone(zoneB.getId(), mine), otherOwnerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_014"));

        endRequest();
        assertEquals(zoneA.getId(), artworks.findById(mine.getId()).orElseThrow().getExhibitionZone().getId());
    }

    private Account account(Role role) {
        return accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
    }

    // 전시가 없는 전시 어드민 계정은 인증에서 거절되므로 자기 전시를 하나 만들어 둔다
    private String otherOwnerToken() {
        Account otherOwner = account(Role.EXHIBITION_ADMIN);
        exhibition(otherOwner);
        return token(otherOwner);
    }

    private Artist artist(String name) {
        return artists.saveAndFlush(Artist.builder().account(account(Role.ARTIST_ADMIN)).nameKo(name).build());
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private Exhibition exhibition(Account owner) {
        return exhibitions.saveAndFlush(Exhibition.builder().account(owner).univName("테스트 대학")
                .deptName("테스트 학과").slug("manage-" + UUID.randomUUID()).build());
    }

    private ExhibitionZone zone(Exhibition exhibition, String name, int orderId) {
        return zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name(name).orderId(orderId).build());
    }

    private Artwork artworkOf(Artist artist, String title) {
        Artwork artwork = Artwork.builder().title(title).description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private Artwork submitted(Artwork artwork, ExhibitionZone zone, int orderIndex) {
        artwork.submitTo(exhibition, zone);
        artwork.updateOrder(orderIndex);
        return artworks.saveAndFlush(artwork);
    }

    private Artwork published(Artwork artwork) {
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        return artworks.saveAndFlush(artwork);
    }

    private Artwork withMainImg(Artwork artwork) {
        artwork.updateMainImg("https://s3/main.webp");
        return artworks.saveAndFlush(artwork);
    }

    private List<Notification> notificationsOf(Artist artist, NotificationType type) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(artist.getAccount().getId(), Pageable.unpaged())
                .stream().filter(notification -> notification.getType() == type).toList();
    }

    private MockHttpServletRequestBuilder list() {
        return get("/api/exhibitions/{id}/artworks/manage", exhibition.getId());
    }

    private MockHttpServletRequestBuilder bulkHidden(boolean hidden, Artwork... targets) {
        return patch("/api/exhibitions/{id}/artworks/hidden", exhibition.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"artworkIds\":" + idsJson(targets) + ",\"hidden\":" + hidden + "}");
    }

    private MockHttpServletRequestBuilder bulkZone(UUID zoneId, Artwork... targets) {
        return patch("/api/exhibitions/{id}/artworks/zone", exhibition.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"artworkIds\":" + idsJson(targets) + ",\"zoneId\":\"" + zoneId + "\"}");
    }

    private String idsJson(Artwork... targets) {
        return java.util.Arrays.stream(targets)
                .map(artwork -> "\"" + artwork.getId() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private <T extends MockHttpServletRequestBuilder> T withToken(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
