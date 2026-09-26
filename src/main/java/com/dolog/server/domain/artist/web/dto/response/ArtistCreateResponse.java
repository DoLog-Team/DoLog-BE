package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;

import java.util.UUID;

public record ArtistCreateResponse(
        UUID artistId
) {
    public static ArtistCreateResponse from(Artist artist) {
        return new ArtistCreateResponse(artist.getId());
    }
}