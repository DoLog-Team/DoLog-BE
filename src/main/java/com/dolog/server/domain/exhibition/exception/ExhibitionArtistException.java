package com.dolog.server.domain.exhibition.exception;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.code.BaseResponseCode;

public class ExhibitionArtistException extends BaseException {

  public ExhibitionArtistException(BaseResponseCode errorCode) {
    super(errorCode);
  }
}