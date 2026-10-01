package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.PlanUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanListResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanUpdateResponse;

import java.util.UUID;

public interface PlanService {
    PlanCreateResponse createPlan(PlanCreateRequest request);
    PlanListResponse getPlans();
    PlanUpdateResponse updatePlan(UUID planId, PlanUpdateRequest request);
}
