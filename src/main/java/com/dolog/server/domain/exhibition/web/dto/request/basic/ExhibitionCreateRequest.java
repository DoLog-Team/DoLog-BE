package com.dolog.server.domain.exhibition.web.dto.request.basic;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionCreateRequest {

    @NotBlank(message = "필수 입력 값(학교명, 학과명)이 누락되었습니다.")
    private String univName;

    @NotBlank(message = "필수 입력 값(학교명, 학과명)이 누락되었습니다.")
    private String deptName;

    @NotNull(message = "필수 입력 값(유형)이 누락되었습니다.")
    private ExhibitionType exhibitionType;

    @NotBlank(message = "필수 입력 값(slug)이 누락되었습니다.")
    private String slug;

    private Boolean isPublic = true;
}
