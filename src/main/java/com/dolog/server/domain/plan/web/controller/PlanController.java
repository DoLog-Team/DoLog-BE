package com.dolog.server.domain.plan.web.controller;

import com.dolog.server.domain.plan.service.PlanService;
import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.PlanUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanListResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "plan")
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "요금제 등록")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping
    public ResponseEntity<SuccessResponse<PlanCreateResponse>> createPlan(
            @Valid @RequestBody PlanCreateRequest request
    ) {
        PlanCreateResponse data = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data, "요금제 등록에 성공했습니다."));
    }

    @Operation(summary = "요금제 목록 조회")
    @GetMapping
    public SuccessResponse<PlanListResponse> getPlans() {
        return SuccessResponse.ok(planService.getPlans(), "요금제 목록 조회에 성공했습니다.");
    }

    @Operation(summary = "요금제 수정")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/{planId}")
    public SuccessResponse<PlanUpdateResponse> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody PlanUpdateRequest request
    ) {
        PlanUpdateResponse data = planService.updatePlan(planId, request);
        return SuccessResponse.ok(data, "요금제 수정에 성공했습니다.");
    }
}
