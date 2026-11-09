package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArtworkTest {

    @Test
    @DisplayName("새 작품은 DRAFT, 숨김 아님, 조회수 0으로 시작한다")
    void newArtworkDefaults() {
        Artwork artwork = Artwork.builder().title("작품").build();

        assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
        assertFalse(artwork.isHidden());
        assertEquals(0L, artwork.getViewCount());
        assertTrue(artwork.getMaterials().isEmpty());
    }

    @Test
    @DisplayName("DRAFT 작품은 두록 URL, 전시 URL 모두 노출되지 않는다")
    void draftIsNotVisible() {
        Artwork artwork = Artwork.builder().build();

        assertFalse(artwork.isVisibleOnDolog());
        assertFalse(artwork.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("PUBLISHED 작품은 두록 URL 에 노출된다. 출품 안 한 작품은 전시 URL 자체가 없다")
    void publishedIsVisible() {
        Artwork artwork = Artwork.builder().build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);

        assertTrue(artwork.isVisibleOnDolog());
        assertFalse(artwork.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("숨긴 PUBLISHED 작품은 두록 URL 에만 노출되고 status 는 그대로다")
    void hiddenPublishedIsVisibleOnlyOnDolog() {
        Artwork artwork = Artwork.builder().build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);

        artwork.hide(now);

        assertTrue(artwork.isHidden());
        assertEquals(now, artwork.getHiddenAt());
        assertEquals(ArtworkStatus.PUBLISHED, artwork.getStatus());
        assertTrue(artwork.isVisibleOnDolog());
        assertFalse(artwork.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("숨김을 풀면 전시 URL 노출이 돌아온다")
    void unhideRestoresExhibitionVisibility() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Artwork artwork = Artwork.builder().exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.hide(LocalDateTime.of(2026, 10, 3, 12, 0));

        artwork.unhide();

        assertFalse(artwork.isHidden());
        assertNull(artwork.getHiddenAt());
        assertTrue(artwork.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("출품된 작품은 전시가 비공개(만료 포함)여도 두록 개인 페이지 노출은 그대로 유지된다")
    void dologVisibilityIsIndependentFromExhibitionState() {
        Exhibition unpublishedExhibition = Exhibition.builder().isPublic(false).build();
        Artwork artwork = Artwork.builder().exhibition(unpublishedExhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);

        assertTrue(artwork.isVisibleOnDolog());
        assertFalse(artwork.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("숨긴 상태에서 DRAFT 로 바꾸면 숨김은 유지된다")
    void draftKeepsHidden() {
        Artwork artwork = Artwork.builder().build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.hide(LocalDateTime.of(2026, 10, 3, 12, 0));

        artwork.changeStatus(ArtworkStatus.DRAFT);

        assertTrue(artwork.isHidden());
        assertFalse(artwork.isVisibleOnDolog());
    }

    @Test
    @DisplayName("재료는 입력 순서대로 1부터 순서가 매겨지고 다시 넣으면 교체된다")
    void replaceMaterialsKeepsInputOrder() {
        Artwork artwork = Artwork.builder().build();
        artwork.replaceMaterials(List.of("백자토", "청화안료"));

        artwork.replaceMaterials(List.of("유약", "백자토", "청화안료"));

        assertEquals(List.of("유약", "백자토", "청화안료"),
                artwork.getMaterials().stream().map(ArtworkMaterial::getName).toList());
        assertEquals(List.of(1, 2, 3),
                artwork.getMaterials().stream().map(ArtworkMaterial::getOrderIndex).toList());
        assertTrue(artwork.getMaterials().stream().allMatch(m -> m.getArtwork() == artwork));
    }

    @Test
    @DisplayName("재료 이름은 앞뒤 공백을 지우고 빈 이름은 버린다")
    void replaceMaterialsDropsBlankNames() {
        Artwork artwork = Artwork.builder().build();

        artwork.replaceMaterials(java.util.Arrays.asList(" 백자토 ", "", null, "  ", "유약"));

        assertEquals(List.of("백자토", "유약"),
                artwork.getMaterials().stream().map(ArtworkMaterial::getName).toList());
        assertEquals(List.of(1, 2),
                artwork.getMaterials().stream().map(ArtworkMaterial::getOrderIndex).toList());
    }

    @Test
    @DisplayName("전시에만 공개였던 작품은 출품 취소하면 공개로 넘어가지 않고 비공개(DRAFT)로 내려간다")
    void cancelExhibitionSubmissionDraftsExhibitionOnlyArtwork() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Artwork artwork = Artwork.builder().exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.updateExhibitionOnly(true);
        assertFalse(artwork.isVisibleOnDolog()); // 전시에만 공개 상태에서는 두록에 안 보임

        artwork.cancelExhibitionSubmission();

        assertNull(artwork.getExhibition());
        assertFalse(artwork.isExhibitionOnly());
        assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
        assertFalse(artwork.isVisibleOnDolog());
    }

    @Test
    @DisplayName("원래부터 공개였던(전시에만 공개가 아닌) 작품은 출품 취소해도 공개 상태가 그대로 유지된다")
    void cancelExhibitionSubmissionKeepsPublishedWhenNotExhibitionOnly() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Artwork artwork = Artwork.builder().exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);

        artwork.cancelExhibitionSubmission();

        assertNull(artwork.getExhibition());
        assertFalse(artwork.isExhibitionOnly());
        assertEquals(ArtworkStatus.PUBLISHED, artwork.getStatus());
        assertTrue(artwork.isVisibleOnDolog());
    }

    @Test
    @DisplayName("전시에만 공개인 작품은 전시가 비공개로 전환되면 status는 그대로여도 어디에도 안 보인다")
    void visibleAnywhereFalseWhenExhibitionOnlyExhibitionGoesPrivate() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Artwork artwork = Artwork.builder().exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.updateExhibitionOnly(true);
        assertTrue(artwork.isVisibleAnywhere()); // 전시가 공개인 동안은 전시 경유로 보임

        exhibition.unpublish();

        assertEquals(ArtworkStatus.PUBLISHED, artwork.getStatus()); // status는 그대로
        assertFalse(artwork.isVisibleAnywhere()); // 하지만 실제로는 어디에도 안 보임
    }

    @Test
    @DisplayName("전시 자체가 삭제될 때는 출품 취소와 달리 공개로 돌아가지 않고 DRAFT로 내려간다")
    void removeFromDeletedExhibitionDraftsArtwork() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Artwork artwork = Artwork.builder().exhibition(exhibition).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.updateExhibitionOnly(true);

        artwork.removeFromDeletedExhibition();

        assertNull(artwork.getExhibition());
        assertFalse(artwork.isExhibitionOnly());
        assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
        assertFalse(artwork.isVisibleOnDolog());
    }

    @Test
    @DisplayName("PATCH 용 update 메서드는 null 인 값은 건드리지 않는다")
    void updateMethodsIgnoreNull() {
        Artwork artwork = Artwork.builder().title("제목").category("분류").description("설명")
                .shortIntro("소개").purchaseUrl("https://buy").showPurchaseButton(true).build();

        artwork.updateText(null, "새 분류", null, null);
        artwork.updatePurchaseInfo(null, "https://chat", null, null);
        artwork.updateMainImg(null);

        assertEquals("제목", artwork.getTitle());
        assertEquals("새 분류", artwork.getCategory());
        assertEquals("설명", artwork.getDescription());
        assertEquals("소개", artwork.getShortIntro());
        assertEquals("https://buy", artwork.getPurchaseUrl());
        assertEquals("https://chat", artwork.getPurchaseChatUrl());
        assertTrue(artwork.getShowPurchaseButton());
        assertNull(artwork.getMainImg());
    }

    @Test
    @DisplayName("updateSize는 null은 유지하고, clear 플래그가 true인 값만 독립적으로 지운다")
    void updateSizeClearFlagsClearOnlyFlaggedFields() {
        Artwork artwork = Artwork.builder()
                .width(new BigDecimal("10")).height(new BigDecimal("20")).depth(new BigDecimal("30")).build();

        // null만 보내면 (clear 플래그 없음) 기존 값 유지
        artwork.updateSize(null, null, null, false, false, false);
        assertEquals(new BigDecimal("10"), artwork.getWidth());
        assertEquals(new BigDecimal("20"), artwork.getHeight());
        assertEquals(new BigDecimal("30"), artwork.getDepth());

        // height만 지우면 width/depth는 그대로
        artwork.updateSize(null, null, null, false, true, false);
        assertEquals(new BigDecimal("10"), artwork.getWidth());
        assertNull(artwork.getHeight());
        assertEquals(new BigDecimal("30"), artwork.getDepth());

        // 새 값과 clear를 같이 보내면 clear가 우선한다
        artwork.updateSize(new BigDecimal("99"), null, null, true, false, false);
        assertNull(artwork.getWidth());
    }
}
