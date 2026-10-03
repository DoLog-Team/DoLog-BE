package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PlanActiveStatusResponse {

    private UUID planId;
    private Boolean isActive;
    private LocalDateTime updatedAt;

    public static PlanActiveStatusResponse from(Plan plan) {
        return PlanActiveStatusResponse.builder()
                .planId(plan.getId())
                .isActive(plan.getIsActive())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
