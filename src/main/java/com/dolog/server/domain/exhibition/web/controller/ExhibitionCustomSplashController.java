package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionCustomSplashService;
import com.dolog.server.domain.exhibition.web.dto.request.custom.ExhibitionCustomSplashRequest;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomSplashResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition-custom-splash")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionCustomSplashController {

    private final ExhibitionCustomSplashService exhibitionCustomSplashService;

    // 전시회 커스텀 스플래시 설정
    @Operation(summary = "전시회 커스텀 스플래시 설정")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PutMapping("/{exhibitionId}/custom/splash")
    public SuccessResponse<ExhibitionCustomSplashResponse> upsertCustomSplash(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionCustomSplashRequest request
    ) {
        ExhibitionCustomSplashResponse data = exhibitionCustomSplashService.upsertCustomSplash(exhibitionId, request.getSplashImg());
        return SuccessResponse.ok(data, "스플래시 이미지 설정이 완료되었습니다.");
    }
}
