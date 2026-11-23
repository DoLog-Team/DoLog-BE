package com.dolog.server.domain.maintenance.web.dto.response;

import com.dolog.server.domain.maintenance.entity.MaintenanceMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MaintenanceModeResponse {

    private Boolean isActive;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public static MaintenanceModeResponse from(MaintenanceMode mode) {
        return MaintenanceModeResponse.builder()
                .isActive(mode.getIsActive())
                .startAt(mode.getStartAt())
                .endAt(mode.getEndAt())
                .build();
    }
}
