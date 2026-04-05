package com.dolog.server.domain.banner.web.dto.response;

import com.dolog.server.domain.banner.entity.MainBanner;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MainBannerResponse {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer orderIndex;

    public static MainBannerResponse from(MainBanner mainBanner) {
        return MainBannerResponse.builder()
                .id(mainBanner.getId())
                .imageUrl(mainBanner.getImageUrl())
                .linkUrl(mainBanner.getLinkUrl())
                .orderIndex(mainBanner.getOrderIndex())
                .build();
    }
}
