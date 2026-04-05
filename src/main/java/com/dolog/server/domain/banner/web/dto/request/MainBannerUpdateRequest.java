package com.dolog.server.domain.banner.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MainBannerUpdateRequest {

    @NotBlank
    private String imageUrl;

    @NotBlank
    private String linkUrl;

    @NotNull
    private Integer orderIndex;
}
