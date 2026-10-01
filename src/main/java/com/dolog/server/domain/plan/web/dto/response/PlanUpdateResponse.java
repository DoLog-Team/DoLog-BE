package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.PlanTargetSize;
import com.dolog.server.domain.plan.entity.enums.TargetSize;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class PlanUpdateResponse {

    private UUID planId;
    private String name;
    private Integer maxArtworkCount;
    private List<TargetSize> targetSizes;
    private LocalDateTime updatedAt;

    public static PlanUpdateResponse from(Plan plan) {
        return PlanUpdateResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .maxArtworkCount(plan.getMaxArtworkCount())
                .targetSizes(plan.getTargetSizes().stream()
                        .map(PlanTargetSize::getTargetSize)
                        .sorted()
                        .collect(Collectors.toList()))
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
