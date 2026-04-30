package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionGuideMapService;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "exhibition-guide-map")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionGuideMapController {

    private final ExhibitionGuideMapService exhibitionGuideMapService;

    // 관람 안내도(이미지) 등록
    @Operation(summary = "전시 구역 지도 일괄 등록")
    @PostMapping("/{exhibitionId}/guide-maps")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> createGuideMaps(
            @PathVariable UUID exhibitionId,
            @RequestBody List<ExhibitionGuideMapCreateRequest> requests) {

        exhibitionGuideMapService.createGuideMaps(exhibitionId, requests);
        return SuccessResponse.ok(null, "관람 안내 지도가 성공적으로 등록되었습니다.");
    }
}
