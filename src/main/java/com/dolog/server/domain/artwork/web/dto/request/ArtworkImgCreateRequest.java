package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtworkImgCreateRequest {
    @NotNull(message = "이미지 파일은 필수입니다.")
    private MultipartFile imageFile;

    private String description;
    private Integer orderIndex;
}
