package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class ArtistAlreadyExistsException extends BaseException {
    public ArtistAlreadyExistsException() {
        super(ArtistErrorCode.ARTIST_409_ALREADY_EXISTS);
    }
}
