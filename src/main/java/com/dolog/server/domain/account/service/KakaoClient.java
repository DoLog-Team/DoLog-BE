package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.SocialProvider;
import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoClient extends OAuthCodeClient {
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    public KakaoClient(@Qualifier("socialRestTemplate") RestTemplate http,
                       @Value("${oauth.kakao.rest-api-key:}") String clientId,
                       @Value("${oauth.kakao.client-secret:}") String clientSecret,
                       @Value("${oauth.kakao.redirect-uris:}") String redirectUris) {
        super(http, TOKEN_URL, USER_URL, clientId, clientSecret, redirectUris);
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    protected SocialLoginErrorCode classifyTokenError(int status, JsonNode body) {
        String errorCode = text(body, "error_code");
        if ("KOE320".equals(errorCode) || "KOE303".equals(errorCode)) {
            return SocialLoginErrorCode.INVALID_AUTHORIZATION_CODE;
        }
        if ("KOE237".equals(errorCode)) {
            return SocialLoginErrorCode.PROVIDER_UNAVAILABLE;
        }
        return SocialLoginErrorCode.SERVER_CONFIGURATION;
    }

    @Override
    protected SocialProfile toProfile(JsonNode user) {
        String id = text(user, "id");
        if (id == null) {
            return null;
        }
        JsonNode account = user.path("kakao_account");
        String email = account.path("is_email_valid").asBoolean(false)
                && account.path("is_email_verified").asBoolean(false) ? text(account, "email") : null;
        return new SocialProfile(id, text(account.path("profile"), "nickname"), email);
    }
}
