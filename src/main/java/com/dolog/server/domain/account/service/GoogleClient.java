package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.SocialProvider;
import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// ID token은 검증하지 않는다. 서버가 직접 받은 access token으로 UserInfo를 조회해 신원을 확인한다.
@Component
public class GoogleClient extends OAuthCodeClient {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    public GoogleClient(@Qualifier("socialRestTemplate") RestTemplate http,
                        @Value("${oauth.google.client-id:}") String clientId,
                        @Value("${oauth.google.client-secret:}") String clientSecret,
                        @Value("${oauth.google.redirect-uris:}") String redirectUris) {
        super(http, TOKEN_URL, USER_URL, clientId, clientSecret, redirectUris);
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    protected SocialLoginErrorCode classifyTokenError(int status, JsonNode body) {
        // invalid_grant: 만료·재사용된 코드 또는 인가 요청과 다른 redirect_uri
        if ("invalid_grant".equals(text(body, "error"))) {
            return SocialLoginErrorCode.INVALID_AUTHORIZATION_CODE;
        }
        return SocialLoginErrorCode.SERVER_CONFIGURATION;
    }

    @Override
    protected SocialProfile toProfile(JsonNode user) {
        String sub = text(user, "sub");
        if (sub == null) {
            return null;
        }
        String email = user.path("email_verified").asBoolean(false) ? text(user, "email") : null;
        return new SocialProfile(sub, text(user, "name"), email);
    }
}
