package com.dolog.server.domain.artist.web.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistUpdateRequestValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("PATCH 요청에서 nameKo를 생략할 수 있다")
    void allowsMissingNameKo() throws Exception {
        ArtistUpdateRequest request = objectMapper.readValue(
                "{\"phone\":\"01012345678\"}",
                ArtistUpdateRequest.class
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("nameKo가 빈 문자열이나 공백이면 검증에 실패한다")
    void rejectsBlankNameKo() throws Exception {
        ArtistUpdateRequest emptyName = objectMapper.readValue(
                "{\"nameKo\":\"\"}",
                ArtistUpdateRequest.class
        );
        ArtistUpdateRequest whitespaceName = objectMapper.readValue(
                "{\"nameKo\":\"   \"}",
                ArtistUpdateRequest.class
        );

        assertFalse(validator.validate(emptyName).isEmpty());
        assertFalse(validator.validate(whitespaceName).isEmpty());
    }

    @Test
    @DisplayName("공백이 아닌 nameKo는 수정 요청에 사용할 수 있다")
    void acceptsNonBlankNameKo() throws Exception {
        ArtistUpdateRequest request = objectMapper.readValue(
                "{\"nameKo\":\" 김지우 \"}",
                ArtistUpdateRequest.class
        );

        assertTrue(validator.validate(request).isEmpty());
    }
}
