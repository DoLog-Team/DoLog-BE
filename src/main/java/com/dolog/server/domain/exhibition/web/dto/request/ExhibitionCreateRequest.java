package com.dolog.server.domain.exhibition.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionCreateRequest {

    @NotBlank(message = "필수 입력 값(학교명, 학과명)이 누락되었습니다.")
    private String univName;

    @NotBlank(message = "필수 입력 값(학교명, 학과명)이 누락되었습니다.")
    private String deptName;

    private Boolean isPublic = true;
}
