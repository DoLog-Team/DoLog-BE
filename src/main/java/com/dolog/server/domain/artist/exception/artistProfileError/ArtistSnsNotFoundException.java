package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.exception.BaseException;

public class ArtistSnsNotFoundException extends BaseException {
    public ArtistSnsNotFoundException() {
        super(ArtistProfileErrorCode.ARTIST_SNS_404_NOT_FOUND);
    }
}
