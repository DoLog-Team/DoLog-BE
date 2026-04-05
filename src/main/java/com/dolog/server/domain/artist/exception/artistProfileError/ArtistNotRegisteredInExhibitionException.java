package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistNotRegisteredInExhibitionException extends BaseException {

    public ArtistNotRegisteredInExhibitionException() {
        super(ArtistProfileErrorCode.ARTIST_PROFILE_400_NOT_REGISTERED_IN_EXHIBITION);
    }
}