package com.dolog.server.domain.artist.web.dto.response;

import java.util.UUID;

public record ArtistJoinCodeValidateResponse(
        UUID exhibitionId,
        String exhibitionTitle
) {
}
