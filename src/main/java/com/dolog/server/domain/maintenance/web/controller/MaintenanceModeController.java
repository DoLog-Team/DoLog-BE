package com.dolog.server.domain.maintenance.web.controller;

import com.dolog.server.domain.maintenance.service.MaintenanceModeService;
import com.dolog.server.domain.maintenance.web.dto.request.MaintenanceModeUpdateRequest;
import com.dolog.server.domain.maintenance.web.dto.response.MaintenanceModeResponse;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "maintenance")
@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceModeController {

    private final MaintenanceModeService maintenanceModeService;

    @Operation(summary = "점검 모드 상태 조회", description = "인증 없이 누구나 조회 가능합니다.")
    @GetMapping
    public SuccessResponse<MaintenanceModeResponse> getStatus() {
        return SuccessResponse.ok(maintenanceModeService.getStatus(), "점검 모드 상태 조회에 성공했습니다.");
    }

    @Operation(summary = "점검 모드 상태 변경", description = "두록 어드민 전용. isActive=true면 startAt/endAt을 함께 받아 점검을 시작하고, false면 즉시 종료합니다.")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping
    public SuccessResponse<MaintenanceModeResponse> updateStatus(
            @Valid @RequestBody MaintenanceModeUpdateRequest request
    ) {
        MaintenanceModeResponse data = maintenanceModeService.updateStatus(request);
        return SuccessResponse.ok(data, "점검 모드 상태 변경에 성공했습니다.");
    }
}
