package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.web.dto.request.PlanActiveStatusRequest;
import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.PlanUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanActiveStatusResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanAdminListResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanListResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanUpdateResponse;

import java.util.UUID;

public interface PlanService {
    PlanCreateResponse createPlan(PlanCreateRequest request);
    PlanListResponse getPlans();
    PlanAdminListResponse getPlansForAdmin();
    PlanUpdateResponse updatePlan(UUID planId, PlanUpdateRequest request);
    PlanActiveStatusResponse updateActiveStatus(UUID planId, PlanActiveStatusRequest request);
}
