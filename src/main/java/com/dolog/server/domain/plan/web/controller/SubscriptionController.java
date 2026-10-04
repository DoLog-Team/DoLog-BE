package com.dolog.server.domain.plan.web.controller;

import com.dolog.server.domain.plan.entity.enums.RefundStatus;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.service.SubscriptionService;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPlanChangeRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPolicyUpdateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionStatusUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCancelResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionMyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPlanChangeResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPolicyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionRefundResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionStatusUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "전체 구독 현황 조회 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @GetMapping("/subscriptions")
    public SuccessResponse<List<SubscriptionMyResponse>> getSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status
    ) {
        List<SubscriptionMyResponse> data = subscriptionService.getSubscriptions(status);
        return SuccessResponse.ok(data, "구독 현황 조회에 성공했습니다.");
    }

    @Operation(summary = "구독 상태 수동 조정 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/subscriptions/{subscriptionId}/status")
    public SuccessResponse<SubscriptionStatusUpdateResponse> updateStatus(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionStatusUpdateRequest request
    ) {
        SubscriptionStatusUpdateResponse data = subscriptionService.updateStatus(subscriptionId, request);
        return SuccessResponse.ok(data, "구독 상태가 변경되었습니다.");
    }

    @Operation(summary = "구독 해지 유예기간 조회 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @GetMapping("/subscriptions/policy")
    public SuccessResponse<SubscriptionPolicyResponse> getCancelPolicy() {
        return SuccessResponse.ok(subscriptionService.getCancelPolicy(), "구독 정책 조회에 성공했습니다.");
    }

    @Operation(summary = "구독 해지 유예기간 변경 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/subscriptions/policy")
    public SuccessResponse<SubscriptionPolicyResponse> updateCancelPolicy(
            @Valid @RequestBody SubscriptionPolicyUpdateRequest request
    ) {
        return SuccessResponse.ok(subscriptionService.updateCancelPolicy(request), "구독 정책이 변경되었습니다.");
    }

    @Operation(summary = "환불 목록 조회 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @GetMapping("/subscriptions/refunds")
    public SuccessResponse<List<SubscriptionRefundResponse>> getRefunds(
            @RequestParam(required = false) RefundStatus status
    ) {
        return SuccessResponse.ok(subscriptionService.getRefunds(status), "환불 목록 조회에 성공했습니다.");
    }

    @Operation(summary = "환불 완료 처리 (두록 어드민)")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/subscriptions/refunds/{refundId}/complete")
    public SuccessResponse<SubscriptionRefundResponse> completeRefund(@PathVariable UUID refundId) {
        return SuccessResponse.ok(subscriptionService.completeRefund(refundId), "환불 완료 처리되었습니다.");
    }
}
