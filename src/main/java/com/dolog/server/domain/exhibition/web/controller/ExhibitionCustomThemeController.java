package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionService;
import com.dolog.server.domain.exhibition.web.dto.response.basic.ExhibitionCustomThemeResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibition")
@RequiredArgsConstructor
public class ExhibitionCustomThemeController {

    private final ExhibitionService exhibitionService;

    // 전시회 커스텀 설정 조회
    @GetMapping("/{exhibitionId}/custom")
    public SuccessResponse<ExhibitionCustomThemeResponse> getCustomTheme(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionService.getCustomTheme(exhibitionId), "전시회 커스텀 설정 조회가 완료되었습니다.");
    }
}
