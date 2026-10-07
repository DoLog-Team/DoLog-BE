package com.dolog.server.domain.artist.web.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistCreateRequestValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("작가 등록에는 기존 계정의 accountId와 국문 성명이 필요하다")
    void requiresAccountIdAndNameKo() throws Exception {
        ArtistCreateRequest request = objectMapper.readValue(
                "{}",
                ArtistCreateRequest.class
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("accountId와 국문 성명이 있으면 작가 등록 요청으로 사용할 수 있다")
    void acceptsRequiredCreateFields() throws Exception {
        ArtistCreateRequest request = objectMapper.readValue(
                """
                        {
                          "accountId": "%s",
                          "nameKo": "김두록"
                        }
                        """.formatted(UUID.randomUUID()),
                ArtistCreateRequest.class
        );

        assertTrue(validator.validate(request).isEmpty());
    }
}
