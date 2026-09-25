package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class DuplicateArtistPhoneException extends BaseException {
    public DuplicateArtistPhoneException() {
        super(ArtistErrorCode.ARTIST_409_DUPLICATE_PHONE);
    }
}
