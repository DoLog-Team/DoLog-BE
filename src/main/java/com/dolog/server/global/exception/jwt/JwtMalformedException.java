package com.dolog.server.global.exception.jwt;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.code.JwtErrorCode;


public class JwtMalformedException extends BaseException {
    public JwtMalformedException() {
        super(JwtErrorCode.JWT_401_MALFORMED);
    }
}
