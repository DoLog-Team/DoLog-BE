package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtworkImgCreateRequest {
    @NotBlank(message = "상세 이미지 URL은 필수입니다.")
    private String imageUrl;

    private String description;
    private Integer orderIndex;
}
