package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.Valid;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkImgListRequest {
    @Valid
    private List<ArtworkImgCreateRequest> images;
}
