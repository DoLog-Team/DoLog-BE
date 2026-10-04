package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistJoinRequest(
        @NotBlank String joinCode,
        @NotBlank
        @Size(
                max = 300,
                message = "인사말은 300자 이하로 입력해주세요."
        )
        String greeting
) {
}