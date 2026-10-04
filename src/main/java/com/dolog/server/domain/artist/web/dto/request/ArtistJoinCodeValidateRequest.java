package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ArtistJoinCodeValidateRequest(
        @NotBlank String joinCode
) {
}
