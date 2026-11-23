package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtworkShortIntroValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("작품 생성, 수정, 전체 수정의 한줄 소개는 200자까지 허용한다")
    void shortIntroMax200() {
        ArtworkCreateRequest create = new ArtworkCreateRequest();
        ArtworkUpdateRequest update = new ArtworkUpdateRequest();
        ArtworkUpdateFullRequest updateFull = new ArtworkUpdateFullRequest();

        create.setShortIntro("가".repeat(200));
        update.setShortIntro("가".repeat(200));
        updateFull.setShortIntro("가".repeat(200));
        assertTrue(validator.validateProperty(create, "shortIntro").isEmpty());
        assertTrue(validator.validateProperty(update, "shortIntro").isEmpty());
        assertTrue(validator.validateProperty(updateFull, "shortIntro").isEmpty());

        create.setShortIntro("가".repeat(201));
        update.setShortIntro("가".repeat(201));
        updateFull.setShortIntro("가".repeat(201));
        assertFalse(validator.validateProperty(create, "shortIntro").isEmpty());
        assertFalse(validator.validateProperty(update, "shortIntro").isEmpty());
        assertFalse(validator.validateProperty(updateFull, "shortIntro").isEmpty());
    }
}
