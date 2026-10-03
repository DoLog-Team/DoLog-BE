package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPlanChangeRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionStatusUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCancelResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionMyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPlanChangeResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionStatusUpdateResponse;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    SubscriptionCreateResponse createSubscription(UUID exhibitionId, SubscriptionCreateRequest request);
    List<SubscriptionMyResponse> getMySubscriptions(UUID accountId);
    SubscriptionPlanChangeResponse changePlan(UUID exhibitionId, SubscriptionPlanChangeRequest request);
    SubscriptionCancelResponse cancelSubscription(UUID exhibitionId);
    List<SubscriptionMyResponse> getSubscriptions(SubscriptionStatus status);
    SubscriptionStatusUpdateResponse updateStatus(UUID subscriptionId, SubscriptionStatusUpdateRequest request);
}
