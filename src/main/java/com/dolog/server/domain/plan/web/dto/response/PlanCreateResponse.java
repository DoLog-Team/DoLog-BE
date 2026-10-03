package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PlanCreateResponse {

    private UUID planId;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static PlanCreateResponse from(Plan plan) {
        return PlanCreateResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .isActive(plan.getIsActive())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
