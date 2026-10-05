package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistProfileRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("생성 요청의 국문 성명은 한글·영문·숫자·띄어쓰기만 허용한다")
    void validatesCreateNameKoCharacters() {
        ArtistProfileCreateRequest request = validCreateRequest();
        request.setNameKo("김두록!");

        assertFalse(validator.validate(request).isEmpty());

        request.setNameKo("김Dolog 2");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("수정 요청의 영문 성명은 최대 50자의 영문과 띄어쓰기만 허용한다")
    void validatesUpdateNameEn() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();
        request.setNameEn("Dolog 김");

        assertFalse(validator.validate(request).isEmpty());

        request.setNameEn("Dolog Kim");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("프로필 소개는 최대 200자까지 허용한다")
    void validatesBioLength() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();
        request.setBio("가".repeat(201));

        assertFalse(validator.validate(request).isEmpty());

        request.setBio("가".repeat(200));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("PATCH 요청은 모든 수정 필드를 생략할 수 있다")
    void allowsPartialUpdateRequest() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();

        assertTrue(validator.validate(request).isEmpty());
    }

    private ArtistProfileCreateRequest validCreateRequest() {
        ArtistProfileCreateRequest request =
                new ArtistProfileCreateRequest();
        request.setArtistId(UUID.randomUUID());
        request.setExhibitionId(UUID.randomUUID());
        request.setNameKo("김두록");
        return request;
    }
}
