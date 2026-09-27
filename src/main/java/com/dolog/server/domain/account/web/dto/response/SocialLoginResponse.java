package com.dolog.server.domain.account.web.dto.response;

import com.dolog.server.domain.account.entity.enums.Role;

public record SocialLoginResponse(
        boolean isFirstLogin,
        boolean needsTermsAgreement,
        Profile profile,
        Role role,
        String accessToken,
        String refreshToken
) {
    public record Profile(String name, String email) { }
}
