package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.dolog.server.global.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KakaoClient {
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate http;
    private final String clientId;
    private final String clientSecret;
    private final Set<String> redirectUris;

    public KakaoClient(@Qualifier("kakaoRestTemplate") RestTemplate http,
                       @Value("${oauth.kakao.rest-api-key:}") String clientId,
                       @Value("${oauth.kakao.client-secret:}") String clientSecret,
                       @Value("${oauth.kakao.redirect-uris:}") String redirectUris) {
        this.http = http;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUris = Arrays.stream(redirectUris.split(","))
                .map(String::trim).filter(uri -> !uri.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public SocialProfile fetchProfile(String authorizationCode, String redirectUri) {
        if (clientId.isBlank() || clientSecret.isBlank() || redirectUris.isEmpty()) {
            throw new BaseException(SocialLoginErrorCode.SERVER_CONFIGURATION);
        }
        if (!redirectUris.contains(redirectUri)) {
            throw new BaseException(SocialLoginErrorCode.INVALID_REDIRECT_URI);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        JsonNode token;
        try {
            token = http.postForObject(TOKEN_URL, new HttpEntity<>(form, headers), JsonNode.class);
        } catch (HttpClientErrorException e) {
            JsonNode errorBody;
            try {
                errorBody = JSON.readTree(e.getResponseBodyAsByteArray());
            } catch (IOException ignored) {
                errorBody = null;
            }
            String errorCode = text(errorBody, "error_code");
            if ("KOE320".equals(errorCode) || "KOE303".equals(errorCode)) {
                throw new BaseException(SocialLoginErrorCode.INVALID_AUTHORIZATION_CODE);
            }
            if (e.getStatusCode().value() == 429 || "KOE237".equals(errorCode)) {
                throw new BaseException(SocialLoginErrorCode.PROVIDER_UNAVAILABLE);
            }
            throw new BaseException(SocialLoginErrorCode.SERVER_CONFIGURATION);
        } catch (HttpServerErrorException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw connectionError(e);
        } catch (RestClientException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }
        String accessToken = text(token, "access_token");
        if (accessToken == null) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        JsonNode user;
        try {
            user = http.exchange(USER_URL, HttpMethod.GET, new HttpEntity<>(userHeaders), JsonNode.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new BaseException(e.getStatusCode().value() == 429
                    ? SocialLoginErrorCode.PROVIDER_UNAVAILABLE : SocialLoginErrorCode.PROVIDER_ERROR);
        } catch (HttpServerErrorException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw connectionError(e);
        } catch (RestClientException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }

        String id = text(user, "id");
        if (id == null) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }
        JsonNode account = user.path("kakao_account");
        String email = account.path("is_email_valid").asBoolean(false)
                && account.path("is_email_verified").asBoolean(false) ? text(account, "email") : null;
        return new SocialProfile(id, text(account.path("profile"), "nickname"), email);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        String value = node.path(field).asText();
        return value.isBlank() ? null : value;
    }

    private static BaseException connectionError(ResourceAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return new BaseException(SocialLoginErrorCode.PROVIDER_TIMEOUT);
            }
        }
        return new BaseException(SocialLoginErrorCode.PROVIDER_UNAVAILABLE);
    }
}
