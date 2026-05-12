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

    //TODO: 이번 입력 떄는 데이터 등록 수정 편하게 null 값 없애고 나중에 DTO (생성, 수정) 분리해야할 듯

    //@NotBlank(message = "전시회 제목은 필수 입력 항목입니다.")
    private String title;

    //@NotNull(message = "전시회 이미지는 필수 입력 항목입니다.")
    private MultipartFile exhibitionImg;

    //@NotBlank(message = "전시회 설명은 필수 입력 항목입니다.")
    private String description;

    //@NotNull(message = "시작일은 필수 입력 항목입니다.")
    private LocalDate startDate;

    //@NotNull(message = "종료일은 필수 입력 항목입니다.")
    private LocalDate endDate;

    private String dateInfo;

    //@NotBlank(message = "주최 기관 이메일은 필수 입력 항목입니다.")
    private String email;

    private MultipartFile logoImg;

    private String copyright;

    private MultipartFile ogImage;
}
