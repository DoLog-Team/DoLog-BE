package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistProfileNotFoundException extends BaseException {

    public ArtistProfileNotFoundException() {
        super(ArtistProfileErrorCode.ARTIST_PROFILE_404_NOT_FOUND);
    }
}