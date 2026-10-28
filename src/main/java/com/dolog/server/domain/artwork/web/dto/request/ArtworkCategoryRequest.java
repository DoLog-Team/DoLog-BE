package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtworkCategoryRequest(
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Size(max = 100, message = "카테고리 이름은 최대 100자까지 입력할 수 있습니다.")
        String name
) {
}
