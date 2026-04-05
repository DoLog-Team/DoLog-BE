package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistCreateRequest {

    @NotBlank
    private String nameKo;

    private String nameEn;

    private String phone;
}
