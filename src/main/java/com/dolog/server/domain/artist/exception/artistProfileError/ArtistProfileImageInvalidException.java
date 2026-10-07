package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistProfileImageInvalidException extends BaseException {

    public ArtistProfileImageInvalidException() {
        super(ArtistProfileErrorCode.ARTIST_PROFILE_400_INVALID_IMAGE);
    }
}
