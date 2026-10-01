package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;

public interface PlanService {
    PlanCreateResponse createPlan(PlanCreateRequest request);
}
