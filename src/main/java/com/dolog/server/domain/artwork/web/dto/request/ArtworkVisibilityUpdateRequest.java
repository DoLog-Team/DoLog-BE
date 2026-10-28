package com.dolog.server.domain.artwork.web.dto.request;

import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibilityOption;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ArtworkVisibilityUpdateRequest(
        @NotNull ArtworkVisibilityOption visibility,
        // 보내면 그 전시 소속 구역으로 변경한다 (출품 후 그룹을 처음 정하거나 바꿀 때).
        UUID zoneId
) {}
