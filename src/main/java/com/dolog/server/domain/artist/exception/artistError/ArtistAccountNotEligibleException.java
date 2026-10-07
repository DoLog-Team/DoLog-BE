package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class ArtistAccountNotEligibleException extends BaseException {

    public ArtistAccountNotEligibleException() {
        super(ArtistErrorCode.ARTIST_409_ACCOUNT_NOT_ELIGIBLE);
    }
}
