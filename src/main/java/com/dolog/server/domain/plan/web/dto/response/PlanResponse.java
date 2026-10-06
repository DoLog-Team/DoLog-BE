package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.PlanPrice;
import com.dolog.server.domain.plan.entity.PlanTargetSize;
import com.dolog.server.domain.plan.entity.enums.TargetSize;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class PlanResponse {

    private UUID planId;
    private String name;
    private String description;
    private Integer maxArtworkCount;
    private Integer minCommitmentMonths;
    private Boolean isPopular;
    private Integer displayOrder;
    private List<TargetSize> targetSizes;
    private List<PlanPriceResponse> prices;

    public static PlanResponse from(Plan plan) {
        return PlanResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .maxArtworkCount(plan.getMaxArtworkCount())
                .minCommitmentMonths(plan.getMinCommitmentMonths())
                .isPopular(plan.getIsPopular())
                .displayOrder(plan.getDisplayOrder())
                .targetSizes(plan.getTargetSizes().stream()
                        .map(PlanTargetSize::getTargetSize)
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.toList()))
                .prices(plan.getPrices().stream()
                        .sorted(Comparator.comparing(PlanPrice::getMonths))
                        .map(PlanPriceResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
