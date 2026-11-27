package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistProfileRequestValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    @DisplayName("생성 요청의 국문 성명은 기존 숫자·띄어쓰기 허용과 특수문자 제한을 유지한다")
    void validatesCreateNameKoCharacters() {
        ArtistProfileCreateRequest request = validCreateRequest();
        request.setNameKo("김두록!");

        assertFalse(validator.validate(request).isEmpty());

        request.setNameKo("김Dolog 2");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("국문 성명 삭제는 허용하고 기존 특수문자·길이·공백 검증은 유지한다")
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
    @DisplayName("영문 성명은 기존 50자 제한을 유지한다")
    void validatesUpdateNameEn() {
        ArtistProfileUpdateRequest request =
                new ArtistProfileUpdateRequest();
        request.setNameEn("A".repeat(51));

        assertFalse(validator.validate(request).isEmpty());

        request.setNameEn("A".repeat(50));

        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"山田 太郎", "やまだ", "ヤマダ", "王小明", "김두록", "Dolog Kim",
            "José", "Jose\u0301", "अनिता", "สมชาย", "محمد", "Иван"})
    @DisplayName("생성·수정 요청의 두 성명란 모두 언어를 제한하지 않는다")
    void allowsMultilingualNamesForCreateAndUpdate(String name) {
        ArtistProfileCreateRequest createRequest = validCreateRequest();
        createRequest.setNameKo(name);
        createRequest.setNameEn(name);
        assertTrue(validator.validate(createRequest).isEmpty());

        ArtistProfileUpdateRequest updateRequest = new ArtistProfileUpdateRequest();
        updateRequest.setNameKo(name);
        updateRequest.setNameEn(name);
        assertTrue(validator.validate(updateRequest).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("생성 요청의 국문 성명은 기존 필수·공백 검증을 유지한다")
    void rejectsMissingOrBlankCreateNameKo(String name) {
        ArtistProfileCreateRequest request = validCreateRequest();
        request.setNameKo(name);
        assertFalse(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("수정 요청의 국문 성명은 기존 null·빈 문자열 허용을 유지한다")
    void allowsNullOrEmptyUpdateNameKo(String name) {
        ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest();
        request.setNameKo(name);
        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("영문 성명은 생성·수정 시 기존 null·빈 문자열·공백 허용을 유지한다")
    void preservesOptionalNameEnValidation(String name) {
        ArtistProfileCreateRequest createRequest = validCreateRequest();
        createRequest.setNameEn(name);
        assertTrue(validator.validate(createRequest).isEmpty());

        ArtistProfileUpdateRequest updateRequest = new ArtistProfileUpdateRequest();
        updateRequest.setNameEn(name);
        assertTrue(validator.validate(updateRequest).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"山田!", "Jean-Luc", "O'Connor", "山田\n太郎", "山田\t太郎", "山田\r太郎", "山田😀"})
    @DisplayName("두 성명란은 생성·수정 시 기존 특수문자·줄바꿈·탭 제한을 유지한다")
    void preservesRejectedNameCharacters(String name) {
        ArtistProfileCreateRequest createRequest = validCreateRequest();
        createRequest.setNameKo(name);
        assertFalse(validator.validate(createRequest).isEmpty());
        createRequest.setNameKo("김두록");
        createRequest.setNameEn(name);
        assertFalse(validator.validate(createRequest).isEmpty());

        ArtistProfileUpdateRequest updateRequest = new ArtistProfileUpdateRequest();
        updateRequest.setNameKo(name);
        assertFalse(validator.validate(updateRequest).isEmpty());
        updateRequest.setNameKo(null);
        updateRequest.setNameEn(name);
        assertFalse(validator.validate(updateRequest).isEmpty());
    }

    @Test
    @DisplayName("국문란의 숫자 허용과 영문란의 숫자 제한은 생성·수정 시 유지한다")
    void preservesNameDigitValidation() {
        ArtistProfileCreateRequest createRequest = validCreateRequest();
        createRequest.setNameKo("山田 2");
        assertTrue(validator.validate(createRequest).isEmpty());
        createRequest.setNameEn("山田 2");
        assertFalse(validator.validate(createRequest).isEmpty());

        ArtistProfileUpdateRequest updateRequest = new ArtistProfileUpdateRequest();
        updateRequest.setNameKo("山田 2");
        assertTrue(validator.validate(updateRequest).isEmpty());
        updateRequest.setNameEn("山田 2");
        assertFalse(validator.validate(updateRequest).isEmpty());
    }

    @Test
    @DisplayName("생성 요청은 기존 국문 10자·영문 50자 제한을 유지한다")
    void preservesCreateNameLengthLimits() {
        ArtistProfileCreateRequest request = validCreateRequest();
        request.setNameKo("山".repeat(10));
        request.setNameEn("山".repeat(50));
        assertTrue(validator.validate(request).isEmpty());

        request.setNameKo("山".repeat(11));
        assertFalse(validator.validate(request).isEmpty());
        request.setNameKo("山田");
        request.setNameEn("山".repeat(51));
        assertFalse(validator.validate(request).isEmpty());
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
