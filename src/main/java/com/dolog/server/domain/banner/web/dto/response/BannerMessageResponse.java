package com.dolog.server.domain.banner.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BannerMessageResponse {
    private String message;
}
