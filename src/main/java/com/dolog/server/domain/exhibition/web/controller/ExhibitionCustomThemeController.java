package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionCustomThemeService;
import com.dolog.server.domain.exhibition.web.dto.request.custom.ExhibitionCustomThemeRequest;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionCustomThemeController {

    private final ExhibitionCustomThemeService exhibitionCustomThemeService;

    // 전시회 커스텀 테마 설정
    @PutMapping("/{exhibitionId}/custom/theme")
    public SuccessResponse<ExhibitionCustomThemeResponse> upsertCustomTheme(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionCustomThemeRequest request
    ) {
        ExhibitionCustomThemeResponse data = exhibitionCustomThemeService.upsertCustomTheme(exhibitionId, request);
        return SuccessResponse.ok(data, "전시회 테마 및 푸터 설정이 완료되었습니다.");
    }
}
