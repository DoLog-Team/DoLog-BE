package com.dolog.server.domain.exhibition.web.dto.request.zone;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionZoneHiddenUpdateRequest {

    @NotNull
    private Boolean hidden;
}
