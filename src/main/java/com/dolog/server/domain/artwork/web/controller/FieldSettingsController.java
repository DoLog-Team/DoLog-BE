package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.fieldsetting.FieldSettingsService;
import com.dolog.server.domain.artwork.web.dto.request.FieldSettingsRequest;
import com.dolog.server.domain.artwork.web.dto.response.FieldSettingsResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artwork-field-settings")
@RestController
@RequiredArgsConstructor
public class FieldSettingsController {

    private final FieldSettingsService fieldSettingsService;

    @Operation(summary = "작품 항목 설정 조회", description = "설정이 없으면 모든 항목이 선택/노출인 기본값을 돌려줍니다.")
    @GetMapping("/exhibitions/{exhibitionId}/field-settings")
    @PreAuthorize("hasRole('EXHIBITION_ADMIN')")
    public SuccessResponse<FieldSettingsResponse> getFieldSettings(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(fieldSettingsService.get(user.getId(), exhibitionId), "작품 항목 설정 조회 성공");
    }

    @Operation(summary = "작품 항목 설정 수정", description = "필수 항목을 못 채운 공개 작품은 자동으로 비공개(DRAFT) 전환됩니다.")
    @PutMapping("/exhibitions/{exhibitionId}/field-settings")
    @PreAuthorize("hasRole('EXHIBITION_ADMIN')")
    public SuccessResponse<Void> updateFieldSettings(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody FieldSettingsRequest request) {
        fieldSettingsService.update(user.getId(), exhibitionId, request);
        return SuccessResponse.ok(null, "작품 항목 설정 저장 성공");
    }
}
