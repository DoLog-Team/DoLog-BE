package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSnsUpdateRequest {

    @Size(max = 100, message = "SNS 플랫폼 이름은 최대 100자까지 입력할 수 있습니다.")
    @Pattern(
            regexp = "^(?=.*\\S).+$",
            message = "SNS 플랫폼 이름은 빈 값일 수 없습니다."
    )
    private String platformName;

    @Size(max = 255, message = "SNS 주소는 최대 255자까지 입력할 수 있습니다.")
    @Pattern(
            regexp = "(?i)^https?://\\S+$",
            message = "SNS 주소는 http 또는 https URL이어야 합니다."
    )
    private String url;

    @AssertTrue(message = "수정할 SNS 정보를 하나 이상 입력해주세요.")
    public boolean isAnyFieldPresent() {
        return platformName != null || url != null;
    }
}
