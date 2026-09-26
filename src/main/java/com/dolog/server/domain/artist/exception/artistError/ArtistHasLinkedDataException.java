package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.exception.BaseException;

public class ArtistHasLinkedDataException extends BaseException {

    public ArtistHasLinkedDataException() {
        super(ArtistErrorCode.ARTIST_409_LINKED_DATA);
    }
}
