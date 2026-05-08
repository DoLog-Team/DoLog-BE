package com.dolog.server.domain.banner.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MainBannerUpdateRequest {

    private MultipartFile imageFile;

    @NotBlank
    private String linkUrl;

    @NotNull
    private Integer orderIndex;
}
