package com.dolog.server.domain.artist.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistProfileUpdateRequest {

    @Size(
            max = 10,
            message = "국문 성명은 최대 10자까지 입력해주세요."
    )
    @Pattern(
            regexp = "^$|^(?=.*[가-힣A-Za-z0-9])[가-힣A-Za-z0-9 ]+$",
            message = "국문 성명은 한글, 영문, 숫자, 띄어쓰기만 사용할 수 있습니다."
    )
    private String nameKo;

    @Size(max = 50, message = "영문 성명은 최대 50자까지 입력할 수 있습니다.")
    @Pattern(
            regexp = "^[A-Za-z ]*$",
            message = "영문 성명은 영문과 띄어쓰기만 사용할 수 있습니다."
    )
    private String nameEn;

    @Size(max = 200, message = "소개는 최대 200자까지 입력할 수 있습니다.")
    private String bio;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "프로필 이메일. 빈 문자열이면 삭제, 생략 또는 null이면 유지. 계정 이메일은 변경하지 않음")
    private String email;

    @Size(max = 255, message = "구매 문의 주소는 최대 255자까지 입력할 수 있습니다.")
    @Pattern(
            regexp = "^https?://.+$|^$",
            message = "구매 문의 주소는 http 또는 https URL이어야 합니다."
    )
    private String purchaseContactUrl;

    @Schema(description = "true이면 프로필 이미지 삭제. 새 이미지 파일과 함께 전송하면 400")
    private Boolean deleteProfileImg;

}
