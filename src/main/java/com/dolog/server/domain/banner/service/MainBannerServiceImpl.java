package com.dolog.server.domain.banner.service;

import com.dolog.server.domain.banner.entity.MainBanner;
import com.dolog.server.domain.banner.exception.BannerErrorCode;
import com.dolog.server.domain.banner.exception.BannerException;
import com.dolog.server.domain.banner.repository.MainBannerRepository;
import com.dolog.server.domain.banner.web.dto.request.MainBannerUpdateRequest;
import com.dolog.server.domain.banner.web.dto.response.BannerMessageResponse;
import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainBannerServiceImpl implements MainBannerService {

    private final MainBannerRepository mainBannerRepository;

    @Override
    public List<MainBannerResponse> getMainBanners() {
        return mainBannerRepository.findAllByIsVisibleTrueOrderByOrderIndexAsc()
                .stream()
                .map(MainBannerResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MainBannerResponse updateMainBanner(Long id, MainBannerUpdateRequest request) {
        MainBanner banner = mainBannerRepository.findById(id)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        banner.update(request.getImageUrl(), request.getLinkUrl(), request.getOrderIndex());

        return MainBannerResponse.from(banner);
    }

    @Override
    @Transactional
    public BannerMessageResponse deleteMainBanner(Long id) {
        MainBanner banner = mainBannerRepository.findById(id)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        mainBannerRepository.delete(banner);

        return BannerMessageResponse.builder()
                .message("이미지가 삭제되었습니다")
                .build();
    }
}
