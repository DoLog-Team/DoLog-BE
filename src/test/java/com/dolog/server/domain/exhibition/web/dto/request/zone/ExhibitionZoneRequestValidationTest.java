package com.dolog.server.domain.exhibition.web.dto.request.zone;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExhibitionZoneRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("그룹 일괄 저장의 그룹 설명은 1000자까지 허용한다")
    void bulkSaveDescriptionMax1000() throws Exception {
        assertTrue(validator.validate(bulkRequest("가".repeat(1000))).isEmpty());
        assertFalse(validator.validate(bulkRequest("가".repeat(1001))).isEmpty());
    }

    @Test
    @DisplayName("그룹 생성, 수정의 그룹 설명은 1000자까지 허용한다")
    void createAndUpdateDescriptionMax1000() {
        assertTrue(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "그룹", "desc", "가".repeat(1000)), ExhibitionZoneCreateRequest.class),
                "description").isEmpty());
        assertFalse(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "그룹", "desc", "가".repeat(1001)), ExhibitionZoneCreateRequest.class),
                "description").isEmpty());
        assertTrue(validator.validateProperty(
                objectMapper.convertValue(Map.of("desc", "가".repeat(1000)), ExhibitionZoneUpdateRequest.class),
                "description").isEmpty());
        assertFalse(validator.validateProperty(
                objectMapper.convertValue(Map.of("desc", "가".repeat(1001)), ExhibitionZoneUpdateRequest.class),
                "description").isEmpty());
    }

    @Test
    @DisplayName("그룹 이름은 일괄 저장, 생성, 수정 모두 100자까지 허용한다")
    void nameMax100() {
        assertTrue(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "가".repeat(100), "orderId", 1), ExhibitionZoneBulkSaveRequest.ZoneItem.class),
                "name").isEmpty());
        assertFalse(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "가".repeat(101), "orderId", 1), ExhibitionZoneBulkSaveRequest.ZoneItem.class),
                "name").isEmpty());
        assertFalse(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "가".repeat(101)), ExhibitionZoneCreateRequest.class),
                "name").isEmpty());
        assertTrue(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "가".repeat(100)), ExhibitionZoneUpdateRequest.class),
                "name").isEmpty());
        assertFalse(validator.validateProperty(
                objectMapper.convertValue(Map.of("name", "가".repeat(101)), ExhibitionZoneUpdateRequest.class),
                "name").isEmpty());
    }

    private ExhibitionZoneBulkSaveRequest bulkRequest(String description) throws Exception {
        return objectMapper.convertValue(
                Map.of("zones", java.util.List.of(Map.of("name", "그룹", "orderId", 1, "description", description))),
                ExhibitionZoneBulkSaveRequest.class);
    }
}
