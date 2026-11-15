package com.dolog.server.domain.account.web.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExhibitionLoginRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("전시 어드민 로그인 코드는 500자까지 허용한다")
    void entryCodeMax500() {
        assertTrue(validator.validate(request("A".repeat(500))).isEmpty());
        assertFalse(validator.validate(request("A".repeat(501))).isEmpty());
    }

    private ExhibitionLoginRequest request(String entryCode) {
        return objectMapper.convertValue(Map.of("entryCode", entryCode), ExhibitionLoginRequest.class);
    }
}
