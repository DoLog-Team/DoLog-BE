package com.dolog.server.domain.exhibition.support;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.entity.Host;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExhibitionPublishRequirementTest {

    private final ExhibitionPublishRequirement requirement = new ExhibitionPublishRequirement();

    @Test
    @DisplayName("8개 필수값이 모두 채워지면 missingFields가 비고 isSatisfied는 true다")
    void satisfiedWhenAllFieldsFilled() {
        Exhibition exhibition = Exhibition.builder()
                .id(UUID.randomUUID())
                .exhibitionType(ExhibitionType.GRADUATION)
                .build();
        ExhibitionDetail detail = detail("무제", LocalDate.now(), LocalDate.now().plusDays(1), "주제 설명");
        ExhibitionMap map = map("서울시 종로구");
        Host host = host("두록", "소개");

        assertTrue(requirement.missingFields(exhibition, detail, map, host).isEmpty());
        assertTrue(requirement.isSatisfied(exhibition, detail, map, host));
    }

    @Test
    @DisplayName("전시 유형이 없으면 exhibitionType이 누락 목록에 들어간다")
    void missingExhibitionType() {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).build();
        ExhibitionDetail detail = detail("무제", LocalDate.now(), LocalDate.now().plusDays(1), "주제 설명");
        ExhibitionMap map = map("서울시 종로구");
        Host host = host("두록", "소개");

        assertEquals(List.of("exhibitionType"), requirement.missingFields(exhibition, detail, map, host));
        assertFalse(requirement.isSatisfied(exhibition, detail, map, host));
    }

    @Test
    @DisplayName("상세/장소/호스트 정보가 아예 없으면 해당 필드들이 모두 누락 목록에 들어간다")
    void missingWhenRelatedRowsAbsent() {
        Exhibition exhibition = Exhibition.builder()
                .id(UUID.randomUUID())
                .exhibitionType(ExhibitionType.GRADUATION)
                .build();

        assertEquals(
                List.of("title", "startDate", "endDate", "description", "address", "hostName", "hostDescription"),
                requirement.missingFields(exhibition, null, null, null)
        );
    }

    private ExhibitionDetail detail(String title, LocalDate startDate, LocalDate endDate, String description) {
        return ExhibitionDetail.builder()
                .title(title).startDate(startDate).endDate(endDate).description(description)
                .build();
    }

    private ExhibitionMap map(String address) {
        return ExhibitionMap.builder().address(address).build();
    }

    private Host host(String name, String description) {
        return Host.builder().name(name).description(description).build();
    }
}
