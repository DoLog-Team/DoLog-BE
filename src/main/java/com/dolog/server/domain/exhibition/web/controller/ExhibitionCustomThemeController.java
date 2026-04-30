package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionCustomThemeService;
import com.dolog.server.domain.exhibition.service.ExhibitionService;
import com.dolog.server.domain.exhibition.web.dto.request.custom.ExhibitionCustomThemeRequest;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "exhibition-custom-theme")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionCustomThemeController {

    private final ExhibitionCustomThemeService exhibitionCustomThemeService;
    private final ExhibitionService exhibitionService;

    // 전시회 커스텀 테마 설정
    @Operation(summary = "전시회 커스텀 테마 설정")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PutMapping("/{exhibitionId}/custom/theme")
    public SuccessResponse<ExhibitionCustomThemeResponse> upsertCustomTheme(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionCustomThemeRequest request
    ) {
        ExhibitionCustomThemeResponse data = exhibitionCustomThemeService.upsertCustomTheme(exhibitionId, request);
        return SuccessResponse.ok(data, "전시회 테마 및 푸터 설정이 완료되었습니다.");
    }

    // 전시회 커스텀 설정 조회
    @Operation(summary = "전시회 커스텀 설정 통합 조회")
    @GetMapping("/{exhibitionId}/custom")
    public SuccessResponse<ExhibitionCustomThemeResponse> getCustomTheme(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionService.getCustomTheme(exhibitionId), "전시회 커스텀 설정 조회가 완료되었습니다.");
    }
}
