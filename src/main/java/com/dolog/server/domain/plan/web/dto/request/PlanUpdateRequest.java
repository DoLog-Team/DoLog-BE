package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.TargetSize;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PlanUpdateRequest {

    private String name;
    private String description;
    private Integer maxArtworkCount;
    private Integer minCommitmentMonths;
    private Boolean isPopular;
    private Integer displayOrder;
    private List<TargetSize> targetSizes;

    @Valid
    private List<PlanPriceCreateRequest> prices;
}
