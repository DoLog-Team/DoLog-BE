package com.dolog.server.domain.exhibition.web.dto.request;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AddArtistRequest {
    private UUID artistId;
}
