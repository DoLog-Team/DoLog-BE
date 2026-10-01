package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PlanListResponse {

    private List<PlanResponse> plans;

    public static PlanListResponse from(List<Plan> plans) {
        return PlanListResponse.builder()
                .plans(plans.stream().map(PlanResponse::from).collect(Collectors.toList()))
                .build();
    }
}
