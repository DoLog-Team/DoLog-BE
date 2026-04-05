package com.dolog.server.domain.account.exception;

import com.dolog.server.global.exception.BaseException;

public class InvalidPasswordException extends BaseException {

  public InvalidPasswordException() {
    super(AccountErrorCode.INVALID_PASSWORD);
  }
}
