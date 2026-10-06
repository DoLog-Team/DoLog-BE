package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.TargetSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PlanCreateRequest {

    @NotBlank
    private String name;

    private String description;

    private Integer maxArtworkCount;

    private Integer minCommitmentMonths;

    private Boolean isPopular;

    private Integer displayOrder;

    @NotEmpty
    private List<TargetSize> targetSizes;

    @NotEmpty
    @Valid
    private List<PlanPriceCreateRequest> prices;
}
