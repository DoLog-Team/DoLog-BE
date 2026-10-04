package com.dolog.server.domain.exhibition.web.dto.request.artist;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddArtistRequest(
        @NotNull(message = "추가할 작가 ID를 입력해주세요.")
        UUID artistId
) {
}
