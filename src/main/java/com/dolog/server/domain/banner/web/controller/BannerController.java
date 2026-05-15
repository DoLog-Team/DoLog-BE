package com.dolog.server.domain.banner.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.banner.service.MainBannerService;
import com.dolog.server.domain.banner.web.dto.request.MainBannerCreateRequest;
import com.dolog.server.domain.banner.web.dto.request.MainBannerUpdateRequest;
import com.dolog.server.domain.banner.web.dto.response.BannerMessageResponse;
import com.dolog.server.domain.banner.web.dto.response.MainBannerResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition-banner")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class BannerController {

    private final MainBannerService mainBannerService;

    @Operation(summary = "메인 배너 이미지 조회")
    @GetMapping("/mainbanner")
    public ResponseEntity<SuccessResponse<List<MainBannerResponse>>> getMainBanners() {
        List<MainBannerResponse> response = mainBannerService.getMainBanners();
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @Operation(summary = "메인 배너 이미지 생성")
    @PostMapping(value = "/mainbanner", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<MainBannerResponse>> createMainBanner(
            @Valid @ModelAttribute MainBannerCreateRequest request) {
        MainBannerResponse response = mainBannerService.createMainBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    @Operation(summary = "메인 배너 이미지 교체")
    @PutMapping(value = "/mainbanner/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<MainBannerResponse>> updateMainBanner(
            @PathVariable Long id,
            @Valid @ModelAttribute MainBannerUpdateRequest request) {
        MainBannerResponse response = mainBannerService.updateMainBanner(id, request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @Operation(summary = "메인 배너 이미지 삭제")
    @DeleteMapping("/mainbanner/{id}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<BannerMessageResponse>> deleteMainBanner(
            @PathVariable Long id) {
        BannerMessageResponse response = mainBannerService.deleteMainBanner(id);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }
}
