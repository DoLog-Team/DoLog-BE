package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.domain.account.web.dto.response.ExhibitionLoginResponse;
import com.dolog.server.domain.account.web.dto.response.SocialLoginResponse;
import com.dolog.server.domain.account.entity.enums.SocialProvider;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(String email, String password);
    ExhibitionLoginResponse loginExhibition(String entryCode);
    SocialLoginResponse socialLogin(SocialProvider provider, SocialProfile profile);
    TokenResponse refresh(String refreshToken);
    void logout(UUID accountId, long sessionId);
}
