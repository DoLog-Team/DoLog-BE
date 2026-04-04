package com.dolog.server.domain.artist.exception;

import com.dolog.server.global.exception.BaseException;

public class ArtistBadRequestException extends BaseException {

  public ArtistBadRequestException() {
    super(ArtistErrorCode.ARTIST_400_INVALID_REQUEST);
  }
}