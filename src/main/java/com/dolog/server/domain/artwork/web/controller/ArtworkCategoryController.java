package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.ArtworkCategoryService;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCategoryRequest;
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
@Tag(name = "artwork-category")
@RestController
@RequestMapping("/artwork-categories")
@RequiredArgsConstructor
public class ArtworkCategoryController {

    private final ArtworkCategoryService artworkCategoryService;

    @Operation(summary = "작품 카테고리 생성")
    @PostMapping
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> createCategory(@Valid @RequestBody ArtworkCategoryRequest request) {
        artworkCategoryService.createCategory(request);
        return SuccessResponse.ok(null, "카테고리가 성공적으로 생성되었습니다.");
    }

    @Operation(summary = "작품 카테고리 이름 수정")
    @PatchMapping("/{categoryId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ArtworkCategoryRequest request
    ) {
        artworkCategoryService.updateCategory(categoryId, request);
        return SuccessResponse.ok(null, "카테고리가 성공적으로 수정되었습니다.");
    }

    @Operation(summary = "작품 카테고리 삭제")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteCategory(@PathVariable UUID categoryId) {
        artworkCategoryService.deleteCategory(categoryId);
        return SuccessResponse.ok(null, "카테고리가 성공적으로 삭제되었습니다.");
    }

    @Operation(summary = "작품 카테고리 순서 일괄 변경")
    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> reorderCategories(@RequestBody List<ArtworkCategoryReorderRequest> requests) {
        artworkCategoryService.reorderCategories(requests);
        return SuccessResponse.ok(null, "카테고리 순서가 변경되었습니다.");
    }
}
