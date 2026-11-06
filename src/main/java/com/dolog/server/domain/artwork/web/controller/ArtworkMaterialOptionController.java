package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.ArtworkMaterialOptionService;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionRequest;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artwork-material-option")
@RestController
@RequestMapping("/artwork-material-options")
@RequiredArgsConstructor
public class ArtworkMaterialOptionController {

    private final ArtworkMaterialOptionService artworkMaterialOptionService;

    @Operation(summary = "재료 및 기법 옵션 생성")
    @PostMapping
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> createOption(@Valid @RequestBody ArtworkMaterialOptionRequest request) {
        artworkMaterialOptionService.createOption(request);
        return SuccessResponse.ok(null, "재료가 성공적으로 생성되었습니다.");
    }

    @Operation(summary = "재료 및 기법 옵션 이름 수정")
    @PatchMapping("/{materialOptionId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> updateOption(
            @PathVariable UUID materialOptionId,
            @Valid @RequestBody ArtworkMaterialOptionRequest request
    ) {
        artworkMaterialOptionService.updateOption(materialOptionId, request);
        return SuccessResponse.ok(null, "재료가 성공적으로 수정되었습니다.");
    }

    @Operation(summary = "재료 및 기법 옵션 삭제")
    @DeleteMapping("/{materialOptionId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteOption(@PathVariable UUID materialOptionId) {
        artworkMaterialOptionService.deleteOption(materialOptionId);
        return SuccessResponse.ok(null, "재료가 성공적으로 삭제되었습니다.");
    }

    @Operation(summary = "재료 및 기법 옵션 순서 일괄 변경")
    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> reorderOptions(@RequestBody List<ArtworkMaterialOptionReorderRequest> requests) {
        artworkMaterialOptionService.reorderOptions(requests);
        return SuccessResponse.ok(null, "재료 순서가 변경되었습니다.");
    }
}
