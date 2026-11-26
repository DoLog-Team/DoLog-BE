package com.dolog.server.domain.maintenance.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MaintenanceModeUpdateRequest {

    @NotNull
    private Boolean isActive;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
