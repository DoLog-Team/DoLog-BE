package com.dolog.server.domain.plan.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanActiveStatusRequest {

    @NotNull
    private Boolean isActive;
}
