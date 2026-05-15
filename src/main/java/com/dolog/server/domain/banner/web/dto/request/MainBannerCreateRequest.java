package com.dolog.server.domain.banner.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MainBannerCreateRequest {

    @NotNull
    private MultipartFile imageFile;

    private String linkUrl;

    private Integer orderIndex;
}
