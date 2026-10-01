package com.dolog.server.domain.plan.web.controller;

import com.dolog.server.domain.plan.service.SubscriptionService;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
