package com.dolog.server.global.jwt;

import com.dolog.server.global.exception.jwt.JwtExpiredException;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.exception.jwt.JwtMalformedException;
import com.dolog.server.global.exception.jwt.JwtUnsupportedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 60;
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7;
    private final SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID accountId, long sessionId) {
        if (sessionId <= 0) throw new IllegalArgumentException("로그인 세션 ID가 필요합니다.");
        Date now = new Date();
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("token_type", "access")
                .claim("sid", sessionId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID accountId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(accountId.toString())
                .claim("token_type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public record AccessIdentity(UUID accountId, long sessionId) {}

    public AccessIdentity parseAccessToken(String token) {
        Claims claims = parse(token, "access");
        try {
            Long sessionId = claims.get("sid", Long.class);
            if (sessionId == null || sessionId <= 0) throw new JwtInvalidException();
            return new AccessIdentity(UUID.fromString(claims.getSubject()), sessionId);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }

    public UUID parseRefreshToken(String token) {
        return UUID.fromString(parse(token, "refresh").getSubject());
    }

    private Claims parse(String token, String type) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).require("token_type", type)
                    .build().parseSignedClaims(token).getPayload();
            if (claims.getSubject() == null || claims.getExpiration() == null) throw new JwtInvalidException();
            UUID.fromString(claims.getSubject());
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();
        } catch (UnsupportedJwtException e) {
            throw new JwtUnsupportedException();
        } catch (MalformedJwtException e) {
            throw new JwtMalformedException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }
}
