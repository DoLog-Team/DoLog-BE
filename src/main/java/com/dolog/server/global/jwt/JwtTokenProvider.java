package com.dolog.server.global.jwt;

import com.dolog.server.global.exception.jwt.JwtExpiredException;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.exception.jwt.JwtMalformedException;
import com.dolog.server.global.exception.jwt.JwtUnsupportedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

//JWT 토큰 생성 / 검증 유틸리티
@Component
@Slf4j
public class JwtTokenProvider {

    // 토큰 유효기간
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60;       // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7일

    private final Key key;

    // 생성자를 통해 스프링이 yml에서 값을 읽어와 주입해 주고, 안전하게 Key 객체를 생성합니다.
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /** 1. 토큰 생성 */
    /** Access Token */
    public String createAccessToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    /** Refresh Token */
    public String createRefreshToken(String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    /** 2. 토큰 유효성 검증 */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        }  catch (ExpiredJwtException e) {
            log.warn("JWT 만료: {}", e.getMessage());
            throw new JwtExpiredException();

        } catch (UnsupportedJwtException e) {
            log.warn("❌ 지원되지 않는 JWT 형식: {}", e.getMessage());
            throw new JwtUnsupportedException();

        } catch (MalformedJwtException e) {
            log.warn("❌ JWT 구조 손상됨: {}", e.getMessage());
            throw new JwtMalformedException();

        } catch (SignatureException | IllegalArgumentException e) {
            log.warn("❌ JWT 서명 불일치 또는 잘못된 토큰: {}", e.getMessage());
            throw new JwtInvalidException();

        } catch (JwtException e) {
            log.warn("❌ JWT 파싱 중 일반 예외: {}", e.getMessage());
            throw new JwtInvalidException();
        }
    }

    /** 3. 토큰에서 닉네임(subject) 추출 */
    public String getNicknameFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("❌ Access Token 만료: {}", e.getMessage());
            throw new JwtExpiredException();
        } catch (JwtException e) {
            log.warn("❌ JWT 파싱 실패: {}", e.getMessage());
            throw new JwtInvalidException();
        }
    }
}
