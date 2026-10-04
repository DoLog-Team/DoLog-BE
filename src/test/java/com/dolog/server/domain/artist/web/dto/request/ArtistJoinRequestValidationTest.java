package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistJoinRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("참여 신청 인사말은 300자까지 입력할 수 있다")
    void acceptsGreetingAtMaximumLength() {
        ArtistJoinRequest request = new ArtistJoinRequest(
                "2345ABCD",
                "가".repeat(300)
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("참여 신청 인사말은 300자를 초과할 수 없다")
    void rejectsGreetingOverMaximumLength() {
        ArtistJoinRequest request = new ArtistJoinRequest(
                "2345ABCD",
                "가".repeat(301)
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
