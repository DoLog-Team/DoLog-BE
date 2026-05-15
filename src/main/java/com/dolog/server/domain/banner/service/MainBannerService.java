package com.dolog.server.domain.banner.service;

import com.dolog.server.domain.banner.web.dto.request.MainBannerCreateRequest;
import com.dolog.server.domain.banner.web.dto.request.MainBannerUpdateRequest;
import com.dolog.server.domain.banner.web.dto.response.BannerMessageResponse;
import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;

import java.util.List;

public interface MainBannerService {
    List<MainBannerResponse> getMainBanners();
    MainBannerResponse createMainBanner(MainBannerCreateRequest request);
    MainBannerResponse updateMainBanner(Long id, MainBannerUpdateRequest request);
    BannerMessageResponse deleteMainBanner(Long id);
}
