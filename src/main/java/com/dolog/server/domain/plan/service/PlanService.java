package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanListResponse;

public interface PlanService {
    PlanCreateResponse createPlan(PlanCreateRequest request);
    PlanListResponse getPlans();
}
