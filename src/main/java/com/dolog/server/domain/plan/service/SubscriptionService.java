package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;

import java.util.UUID;

public interface SubscriptionService {
    SubscriptionCreateResponse createSubscription(UUID exhibitionId, SubscriptionCreateRequest request);
}
