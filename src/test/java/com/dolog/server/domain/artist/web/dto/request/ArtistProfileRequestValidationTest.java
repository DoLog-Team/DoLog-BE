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
    @DisplayName("국문 성명 삭제는 허용하고 기존 문자·길이·공백 검증은 유지한다")
    void allowsEmptyUpdateNameKoAndPreservesOtherValidation() {
        ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest();
        request.setNameKo("");
        assertTrue(validator.validate(request).isEmpty());

        for (String invalidName : new String[]{"   ", "김두록!", "가".repeat(11)}) {
            request.setNameKo(invalidName);
            assertFalse(validator.validate(request).isEmpty());
        }

        request.setNameKo("가".repeat(10));
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
    @DisplayName("구매 문의 주소(purchaseContactUrl)는 http(s) 스킴이 없는 자유 형식도 허용한다")
    void allowsFreeFormPurchaseContactUrl() {
        ArtistProfileUpdateRequest updateRequest = new ArtistProfileUpdateRequest();
        updateRequest.setPurchaseContactUrl("kakao.com/abc");

        assertTrue(validator.validate(updateRequest).isEmpty());

        ArtistProfileCreateRequest createRequest = validCreateRequest();
        createRequest.setPurchaseContactUrl("kakao.com/abc");

        assertTrue(validator.validate(createRequest).isEmpty());
    }

    @Test
    @DisplayName("PATCH 요청은 모든 수정 필드를 생략할 수 있다")
    void allowsPartialUpdateRequest() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("SNS 추가 요청은 플랫폼 이름과 주소가 필수이지만, 주소 형식(http 등)은 제한하지 않는다")
    void validatesArtistSnsCreateRequest() {
        ArtistSnsRequest request = new ArtistSnsRequest(
                "",
                "instagram.com/dolog"
        );

        assertFalse(validator.validate(request).isEmpty());

        request = new ArtistSnsRequest(
                "instagram",
                "https://instagram.com/dolog"
        );

        assertTrue(validator.validate(request).isEmpty());

        // http(s) 스킴 없는 자유 형식 주소도 허용한다
        request = new ArtistSnsRequest(
                "instagram",
                "@dolog_official"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("SNS 수정 요청은 하나 이상의 유효한 필드를 요구하지만, 주소 형식(http 등)은 제한하지 않는다")
    void validatesArtistSnsUpdateRequest() {
        ArtistSnsUpdateRequest request =
                new ArtistSnsUpdateRequest(null, null);

        assertFalse(validator.validate(request).isEmpty());

        request = new ArtistSnsUpdateRequest(
                null,
                "https://behance.net/dolog"
        );

        assertTrue(validator.validate(request).isEmpty());

        request = new ArtistSnsUpdateRequest(" ", null);

        assertFalse(validator.validate(request).isEmpty());

        // http(s) 스킴 없는 자유 형식 주소도 허용한다
        request = new ArtistSnsUpdateRequest(null, "instagram.com/dolog");

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
