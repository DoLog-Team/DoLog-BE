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
import java.security.Key;
import java.util.Date;

//JWT 토큰 생성 / 검증 유틸리티
@Component
public class JwtTokenProvider {

    // 비밀키 (환경변수에서 읽기, 없을 경우 기본값)
    private final String secretKey;

    // 토큰 유효기간
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60;       // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7일

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret-key:defaultSecretKeyPleaseChangeThisInProductionEnvironmentWith32BytesOr256Bits}") String secretKey) {
        this.secretKey = secretKey;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /** 1. 토큰 생성 */
    /** Access Token */
    public String createAccessToken(String nickname) {
        Date now = new Date();
        return Jwts.builder()
                .subject(nickname)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    /** Refresh Token */
    public String createRefreshToken() {
        Date now = new Date();
        return Jwts.builder()
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
            System.err.println("❌ JWT 만료됨: " + e.getMessage());
            throw new JwtExpiredException();

        } catch (UnsupportedJwtException e) {
            System.err.println("❌ 지원되지 않는 JWT 형식: " + e.getMessage());
            throw new JwtUnsupportedException();

        } catch (MalformedJwtException e) {
            System.err.println("❌ JWT 구조 손상됨: " + e.getMessage());
            throw new JwtMalformedException();

        } catch (SignatureException | IllegalArgumentException e) {
            System.err.println("❌ JWT 서명 불일치 또는 잘못된 토큰: " + e.getMessage());
            throw new JwtInvalidException();

        } catch (JwtException e) {
            System.err.println("❌ JWT 파싱 중 일반 예외: " + e.getMessage());
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
            System.err.println("❌ Access Token 만료: " + e.getMessage());
            throw new JwtExpiredException();
        } catch (JwtException e) {
            System.err.println("❌ JWT 파싱 실패: " + e.getMessage());
            throw new JwtInvalidException();
        }
    }
}
