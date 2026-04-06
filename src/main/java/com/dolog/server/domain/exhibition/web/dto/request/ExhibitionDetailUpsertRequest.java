package com.dolog.server.domain.exhibition.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ExhibitionDetailUpsertRequest {

    @NotBlank(message = "전시회 제목은 필수 입력 항목입니다.")
    private String title;

    private String exhibitionImg;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;
}
