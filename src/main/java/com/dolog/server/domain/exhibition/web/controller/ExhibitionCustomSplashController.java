package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionCustomSplashService;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomSplashResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionCustomSplashController {

    private final ExhibitionCustomSplashService exhibitionCustomSplashService;

    // 전시회 커스텀 스플래시 설정
    @PreAuthorize("hasRole('DEVELOPER')")
    @PutMapping("/{exhibitionId}/custom/splash")
    public SuccessResponse<ExhibitionCustomSplashResponse> upsertCustomSplash(
            @PathVariable UUID exhibitionId,
            @RequestParam(value = "splash_img", required = false) String splashImg
    ) {
        ExhibitionCustomSplashResponse data = exhibitionCustomSplashService.upsertCustomSplash(exhibitionId, splashImg);
        return SuccessResponse.ok(data, "스플래시 이미지 설정이 완료되었습니다.");
    }
}
