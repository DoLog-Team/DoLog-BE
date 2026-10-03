package com.dolog.server.domain.exhibition.web.dto.request.artist;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RemoveArtistRequest {
    @NotNull(message = "제외할 작가 ID를 입력해주세요.")
    private UUID artistId;
}
