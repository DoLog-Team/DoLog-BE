package com.dolog.server.domain.artwork.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArtworkHiddenResponse(
        UUID artworkId,
        boolean hidden,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime hiddenAt
) {}
