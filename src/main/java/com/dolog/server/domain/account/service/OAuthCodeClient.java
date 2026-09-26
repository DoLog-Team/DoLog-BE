package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.SocialProvider;
import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.dolog.server.global.exception.BaseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// 인가 코드 → 공급자 access token → 사용자 프로필 공통 흐름. 공급자별 차이는 하위 클래스가 맡는다.
@Slf4j
public abstract class OAuthCodeClient {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate http;
    private final String tokenUrl;
    private final String userUrl;
    private final String clientId;
    private final String clientSecret;
    private final Set<String> redirectUris;

    protected OAuthCodeClient(RestTemplate http, String tokenUrl, String userUrl,
                              String clientId, String clientSecret, String redirectUris) {
        this.http = http;
        this.tokenUrl = tokenUrl;
        this.userUrl = userUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUris = Arrays.stream(redirectUris.split(","))
                .map(String::trim).filter(uri -> !uri.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public abstract SocialProvider provider();

    // 토큰 교환 4xx 응답을 오류 코드로 분류한다. body는 JSON이 아니면 null.
    protected abstract SocialLoginErrorCode classifyTokenError(int status, JsonNode body);

    // 사용자 조회 응답을 프로필로 바꾼다. 식별자가 없으면 null을 돌려준다.
    protected abstract SocialProfile toProfile(JsonNode user);

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
            token = http.postForObject(tokenUrl, new HttpEntity<>(form, headers), JsonNode.class);
        } catch (HttpClientErrorException e) {
            JsonNode errorBody;
            try {
                errorBody = JSON.readTree(e.getResponseBodyAsByteArray());
            } catch (IOException ignored) {
                errorBody = null;
            }
            int status = e.getStatusCode().value();
            log.warn("{} token exchange failed: status={} body={}", provider(), status, e.getResponseBodyAsString());
            throw new BaseException(status == 429
                    ? SocialLoginErrorCode.PROVIDER_UNAVAILABLE : classifyTokenError(status, errorBody));
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
            user = http.exchange(userUrl, HttpMethod.GET, new HttpEntity<>(userHeaders), JsonNode.class).getBody();
        } catch (HttpClientErrorException e) {
            log.warn("{} userinfo failed: status={} body={}", provider(), e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw new BaseException(e.getStatusCode().value() == 429
                    ? SocialLoginErrorCode.PROVIDER_UNAVAILABLE : SocialLoginErrorCode.PROVIDER_ERROR);
        } catch (HttpServerErrorException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            throw connectionError(e);
        } catch (RestClientException e) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }

        SocialProfile profile = user == null ? null : toProfile(user);
        if (profile == null) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_ERROR);
        }
        return profile;
    }

    protected static String text(JsonNode node, String field) {
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
