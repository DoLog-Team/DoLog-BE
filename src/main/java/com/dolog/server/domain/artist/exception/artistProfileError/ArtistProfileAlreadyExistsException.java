package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistProfileAlreadyExistsException extends BaseException {

    public ArtistProfileAlreadyExistsException() {
        super(ArtistProfileErrorCode.ARTIST_PROFILE_409_ALREADY_EXISTS);
    }
}