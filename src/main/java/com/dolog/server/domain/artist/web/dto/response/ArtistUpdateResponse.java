package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;

import java.util.UUID;

public record ArtistUpdateResponse(UUID artistId) {

    public static ArtistUpdateResponse from(Artist artist) {
        return new ArtistUpdateResponse(artist.getId());
    }
}
