package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPlanChangeRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionMyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPlanChangeResponse;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    SubscriptionCreateResponse createSubscription(UUID exhibitionId, SubscriptionCreateRequest request);
    List<SubscriptionMyResponse> getMySubscriptions(UUID accountId);
    SubscriptionPlanChangeResponse changePlan(UUID exhibitionId, SubscriptionPlanChangeRequest request);
}
