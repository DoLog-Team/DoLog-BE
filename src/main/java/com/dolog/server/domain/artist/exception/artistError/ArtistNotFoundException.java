package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class ArtistNotFoundException extends BaseException {

    public ArtistNotFoundException() {
        super(ArtistErrorCode.ARTIST_404_NOT_FOUND);
    }
}
