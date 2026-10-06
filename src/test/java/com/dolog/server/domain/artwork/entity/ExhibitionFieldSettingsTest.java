package com.dolog.server.domain.artwork.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExhibitionFieldSettingsTest {

    @Test
    @DisplayName("필수 항목이 없으면 빈 작품도 통과한다")
    void noRequirements() {
        assertTrue(ExhibitionFieldSettings.builder().build().isSatisfiedBy(Artwork.builder().build()));
    }

    @Test
    @DisplayName("대표 이미지 필수면 이미지가 있어야 한다")
    void requiresMainImg() {
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.builder().requiredMainImg(true).build();

        assertFalse(settings.isSatisfiedBy(Artwork.builder().build()));
        assertFalse(settings.isSatisfiedBy(Artwork.builder().mainImg(" ").build()));
        assertTrue(settings.isSatisfiedBy(Artwork.builder().mainImg("https://s3/main.webp").build()));
    }

    @Test
    @DisplayName("사이즈 필수면 가로와 세로가 있어야 하고 높이는 없어도 된다")
    void requiresWidthAndHeight() {
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.builder().requiredSize(true).build();

        assertFalse(settings.isSatisfiedBy(Artwork.builder().width(BigDecimal.TEN).build()));
        assertTrue(settings.isSatisfiedBy(Artwork.builder().width(BigDecimal.TEN).height(BigDecimal.ONE).build()));
    }

    @Test
    @DisplayName("재료 필수면 재료가 하나 이상 있어야 한다")
    void requiresMaterials() {
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.builder().requiredMaterials(true).build();
        Artwork artwork = Artwork.builder().build();

        assertFalse(settings.isSatisfiedBy(artwork));
        artwork.replaceMaterials(List.of("백자토"));
        assertTrue(settings.isSatisfiedBy(artwork));
    }

    @Test
    @DisplayName("위치 지도 필수면 위치 지도가 있어야 한다")
    void requiresLocationMap() {
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.builder().requiredLocationMap(true).build();

        assertFalse(settings.isSatisfiedBy(Artwork.builder().build()));
        assertTrue(settings.isSatisfiedBy(Artwork.builder().locationMap("https://s3/map.webp").build()));
    }
}
