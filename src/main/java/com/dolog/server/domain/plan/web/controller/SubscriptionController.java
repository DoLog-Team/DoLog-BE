package com.dolog.server.domain.plan.web.controller;

import com.dolog.server.domain.plan.service.SubscriptionService;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPlanChangeRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCancelResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionMyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPlanChangeResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "subscription")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "전시 구독 신청")
    @PostMapping("/exhibitions/{exhibitionId}/subscriptions")
    public ResponseEntity<SuccessResponse<SubscriptionCreateResponse>> createSubscription(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody SubscriptionCreateRequest request
    ) {
        SubscriptionCreateResponse data = subscriptionService.createSubscription(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(data, "전시 구독 신청이 완료되었습니다."));
    }

    @Operation(summary = "내 전시 구독 조회")
    @GetMapping("/subscriptions/me")
    public SuccessResponse<List<SubscriptionMyResponse>> getMySubscriptions(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<SubscriptionMyResponse> data = subscriptionService.getMySubscriptions(user.getId());
        return SuccessResponse.ok(data, "내 구독 목록 조회에 성공했습니다.");
    }

    @Operation(summary = "구독 플랜 변경")
    @PatchMapping("/exhibitions/{exhibitionId}/subscriptions/current/plan")
    public SuccessResponse<SubscriptionPlanChangeResponse> changePlan(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody SubscriptionPlanChangeRequest request
    ) {
        SubscriptionPlanChangeResponse data = subscriptionService.changePlan(exhibitionId, request);
        return SuccessResponse.ok(data, "구독 플랜 변경이 완료되었습니다.");
    }

    @Operation(summary = "구독 해지")
    @PatchMapping("/exhibitions/{exhibitionId}/subscriptions/current/cancel")
    public SuccessResponse<SubscriptionCancelResponse> cancelSubscription(
            @PathVariable UUID exhibitionId
    ) {
        SubscriptionCancelResponse data = subscriptionService.cancelSubscription(exhibitionId);
        return SuccessResponse.ok(data, "구독 해지가 완료되었습니다.");
    }
}
