package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.domain.account.web.dto.response.ExhibitionLoginResponse;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(String email, String password);
    ExhibitionLoginResponse loginExhibition(String entryCode);
    TokenResponse refresh(String refreshToken);
    void logout(UUID accountId, long sessionId);
}
