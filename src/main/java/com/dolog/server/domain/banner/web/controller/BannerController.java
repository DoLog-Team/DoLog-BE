package com.dolog.server.domain.banner.web.controller;

import com.dolog.server.domain.banner.service.MainBannerService;
import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class BannerController {

    private final MainBannerService mainBannerService;

    @GetMapping("/mainbanner")
    public ResponseEntity<SuccessResponse<List<MainBannerResponse>>> getMainBanners() {
        List<MainBannerResponse> response = mainBannerService.getMainBanners();
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }
}
