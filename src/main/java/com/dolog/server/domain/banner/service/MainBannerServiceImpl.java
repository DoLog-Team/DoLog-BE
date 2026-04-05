package com.dolog.server.domain.banner.service;

import com.dolog.server.domain.banner.repository.MainBannerRepository;
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
}
