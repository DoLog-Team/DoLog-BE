package com.dolog.server.domain.banner.service;

import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;

import java.util.List;

public interface MainBannerService {
    List<MainBannerResponse> getMainBanners();
}
