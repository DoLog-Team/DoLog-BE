package com.dolog.server.domain.account.exception;

import com.dolog.server.global.exception.BaseException;

public class DuplicateEmailException extends BaseException {

  public DuplicateEmailException() {
    super(AccountErrorCode.DUPLICATE_EMAIL);
  }
}