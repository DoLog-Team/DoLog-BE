package com.dolog.server.domain.exhibition.web.dto.request.basic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ExhibitionDetailUpsertRequest {

    @NotBlank(message = "전시회 제목은 필수 입력 항목입니다.")
    private String title;

    @NotNull(message = "전시회 이미지는 필수 입력 항목입니다.")
    private MultipartFile exhibitionImg;

    @NotNull(message = "시작일은 필수 입력 항목입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수 입력 항목입니다.")
    private LocalDate endDate;

    @NotBlank(message = "전시회 설명은 필수 입력 항목입니다.")
    private String description;
}
