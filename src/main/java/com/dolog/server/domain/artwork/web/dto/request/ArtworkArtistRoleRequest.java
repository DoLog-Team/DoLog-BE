package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtworkArtistRoleRequest(
        @NotBlank @Size(max = 100) String artistRole
) {}
