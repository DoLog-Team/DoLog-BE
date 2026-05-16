package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionGuideMapService;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition-guide-map")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionGuideMapController {

    private final ExhibitionGuideMapService exhibitionGuideMapService;

    @Operation(summary = "전시 구역 지도 일괄 등록")
    @PostMapping("/{exhibitionId}/guide-maps")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> createGuideMaps(
            @PathVariable UUID exhibitionId,
            @ModelAttribute ExhibitionGuideMapCreateRequest request
    ) {

        exhibitionGuideMapService.createGuideMaps(
                exhibitionId,
                request.getRequests()
        );

        return SuccessResponse.ok(
                null,
                "관람 안내 지도가 성공적으로 등록되었습니다."
        );
    }
}