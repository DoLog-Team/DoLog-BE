package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExhibitionMyItemResponseTest {

    @Test
    @DisplayName("전시 상세 정보가 있으면 대표 이미지와 기간을 함께 내려준다")
    void includesImageAndPeriodWhenDetailExists() {
        ExhibitionDetail detail = ExhibitionDetail.builder()
                .exhibitionImg("https://s3/poster.webp")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 10))
                .build();
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).exhibitionDetail(detail).build();
        ExhibitionArtistMap map = ExhibitionArtistMap.builder()
                .exhibition(exhibition).status(ExhibitionArtistStatus.JOINED).build();

        ExhibitionMyItemResponse response = ExhibitionMyItemResponse.from(map, "전시명");

        assertEquals("https://s3/poster.webp", response.getExhibitionImg());
        assertEquals(LocalDate.of(2026, 10, 1), response.getStartDate());
        assertEquals(LocalDate.of(2026, 10, 10), response.getEndDate());
    }

    @Test
    @DisplayName("전시 상세 정보가 없으면 대표 이미지와 기간은 null이다")
    void nullWhenDetailMissing() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        ExhibitionArtistMap map = ExhibitionArtistMap.builder()
                .exhibition(exhibition).status(ExhibitionArtistStatus.PENDING).build();

        ExhibitionMyItemResponse response = ExhibitionMyItemResponse.from(map, "전시명");

        assertNull(response.getExhibitionImg());
        assertNull(response.getStartDate());
        assertNull(response.getEndDate());
    }
}
