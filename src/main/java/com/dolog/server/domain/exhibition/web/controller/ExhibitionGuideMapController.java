package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionGuideMapService;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @Operation(summary = "전시 구역 지도 등록")
    @PostMapping(
            value = "/{exhibitionId}/guide-maps",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> createGuideMap(
            @PathVariable UUID exhibitionId,
            @ModelAttribute ExhibitionGuideMapCreateRequest request
    ) {

        exhibitionGuideMapService.createGuideMap(
                exhibitionId,
                request
        );

        return SuccessResponse.ok(
                null,
                "관람 안내 지도가 성공적으로 등록되었습니다."
        );
    }


    @Operation(summary = "전시 구역 지도 수정")
    @PatchMapping(
            value = "/guide-maps/{guideMapId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> updateGuideMap(
            @PathVariable UUID guideMapId,
            @ModelAttribute ExhibitionGuideMapCreateRequest request
    ) {
        exhibitionGuideMapService.updateGuideMap(
                guideMapId,
                request
        );

        return SuccessResponse.ok(
                null,
                "관람 안내 지도가 성공적으로 수정되었습니다."
        );
    }

    @Operation(summary = "전시 구역 지도 삭제")
    @DeleteMapping("/guide-maps/{guideMapId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteGuideMap(
            @PathVariable UUID guideMapId
    ) {
        exhibitionGuideMapService.deleteGuideMap(guideMapId);

        return SuccessResponse.ok(
                null,
                "관람 안내 지도가 성공적으로 삭제되었습니다."
        );
    }
}