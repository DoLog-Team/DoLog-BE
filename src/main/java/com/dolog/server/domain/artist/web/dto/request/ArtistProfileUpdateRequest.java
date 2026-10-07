package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistProfileUpdateRequest {

    @Size(
            min = 1,
            max = 10,
            message = "국문 성명은 1자 이상 10자 이하로 입력해주세요."
    )
    @Pattern(
            regexp = "^(?=.*[가-힣A-Za-z0-9])[가-힣A-Za-z0-9 ]+$",
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
    private String email;

    @Size(max = 255, message = "구매 문의 주소는 최대 255자까지 입력할 수 있습니다.")
    @Pattern(
            regexp = "^https?://.+$|^$",
            message = "구매 문의 주소는 http 또는 https URL이어야 합니다."
    )
    private String purchaseContactUrl;

    private Boolean isPublic;
}
