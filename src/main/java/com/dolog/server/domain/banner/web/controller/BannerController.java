package com.dolog.server.domain.banner.web.controller;

import com.dolog.server.domain.banner.service.MainBannerService;
import com.dolog.server.domain.banner.web.dto.request.MainBannerUpdateRequest;
import com.dolog.server.domain.banner.web.dto.response.BannerMessageResponse;
import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/mainbanner/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<MainBannerResponse>> updateMainBanner(
            @PathVariable Long id,
            @Valid @RequestBody MainBannerUpdateRequest request) {
        MainBannerResponse response = mainBannerService.updateMainBanner(id, request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @DeleteMapping("/mainbanner/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<BannerMessageResponse>> deleteMainBanner(
            @PathVariable Long id) {
        BannerMessageResponse response = mainBannerService.deleteMainBanner(id);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }
}
