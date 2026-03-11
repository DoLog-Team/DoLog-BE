package com.dolog.server.global.exception.jwt;

import com.likelion.server.global.exception.BaseException;
import com.likelion.server.global.response.code.JwtErrorCode;

public class JwtInvalidException extends BaseException {
    public JwtInvalidException() {
        super(JwtErrorCode.JWT_401_INVALID);
    }
}
