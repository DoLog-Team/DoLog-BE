package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistProfileAccessDeniedException extends BaseException {

    public ArtistProfileAccessDeniedException() {
        super(ArtistProfileErrorCode.ARTIST_PROFILE_403_ACCESS_DENIED);
    }
}
