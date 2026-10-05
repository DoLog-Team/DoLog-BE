package com.dolog.server.domain.like.support;

import com.dolog.server.domain.like.exception.LikeErrorCode;
import com.dolog.server.domain.like.exception.LikeException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

// 비로그인 방문자 식별: 쿠키 우선, 없으면 요청 본문(로컬스토리지 값), 둘 다 없으면 새로 발급한다.
@Component
public class VisitorIdResolver {

    public static final String COOKIE_NAME = "visitor_id";
    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Duration MAX_AGE = Duration.ofDays(365);

    public Optional<String> resolve(HttpServletRequest request, String requested) {

        Optional<String> fromCookie = readCookie(request);
        if (fromCookie.isPresent()) {
            return fromCookie;
        }

        if (requested == null || requested.isBlank()) {
            return Optional.empty();
        }

        if (!FORMAT.matcher(requested).matches()) {
            throw new LikeException(LikeErrorCode.INVALID_VISITOR_ID);
        }

        return Optional.of(requested);
    }

    // 쿠키가 없던 방문자에게는 확정된 값을 쿠키로 내려준다 (본문으로 왔거나 새로 발급한 경우).
    public String resolveOrIssue(HttpServletRequest request, HttpServletResponse response, String requested) {

        Optional<String> fromCookie = readCookie(request);
        if (fromCookie.isPresent()) {
            return fromCookie.get();
        }

        String visitorId = resolve(request, requested)
                .orElseGet(() -> "vis_" + UUID.randomUUID().toString().replace("-", ""));

        writeCookie(request, response, visitorId);
        return visitorId;
    }

    private Optional<String> readCookie(HttpServletRequest request) {

        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && FORMAT.matcher(value).matches())
                .findFirst();
    }

    // FE 와 API 도메인이 달라 HTTPS 에서는 SameSite=None 이 필요하다. 로컬 HTTP 는 Secure 를 못 쓰므로 Lax.
    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String visitorId) {

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, visitorId)
                .path("/")
                .maxAge(MAX_AGE)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite(request.isSecure() ? "None" : "Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
