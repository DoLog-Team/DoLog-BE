package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class ArtistAccountNotFoundException extends BaseException {

    public ArtistAccountNotFoundException() {
        super(ArtistErrorCode.ARTIST_404_ACCOUNT_NOT_FOUND);
    }
}
