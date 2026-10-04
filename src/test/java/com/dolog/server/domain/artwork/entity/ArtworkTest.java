package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("PUBLISHED 작품은 두록 URL, 전시 URL 모두 노출된다")
    void publishedIsVisible() {
        Artwork artwork = Artwork.builder().build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);

        assertTrue(artwork.isVisibleOnDolog());
        assertTrue(artwork.isVisibleOnExhibition());
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
        Artwork artwork = Artwork.builder().build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.hide(LocalDateTime.of(2026, 10, 3, 12, 0));

        artwork.unhide();

        assertFalse(artwork.isHidden());
        assertNull(artwork.getHiddenAt());
        assertTrue(artwork.isVisibleOnExhibition());
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
}
