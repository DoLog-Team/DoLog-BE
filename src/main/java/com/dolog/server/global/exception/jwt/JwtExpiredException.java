package com.dolog.server.global.exception.jwt;

import com.likelion.server.global.exception.BaseException;
import com.likelion.server.global.response.code.JwtErrorCode;

public class JwtExpiredException extends BaseException {
    public JwtExpiredException() {
        super(JwtErrorCode.JWT_401_EXPIRED);
    }
}
