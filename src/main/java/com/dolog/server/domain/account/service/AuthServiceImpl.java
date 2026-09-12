package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public LoginResponse login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidLoginException();
        }
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidLoginException::new);

        if (account.getPassword() == null || !passwordEncoder.matches(password, account.getPassword())) {
            throw new InvalidLoginException();
        }

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(account.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getEmail());

        // 동시 로그인 제약을 느슨하게 변경하였습니다
        refreshTokenRepository.save(RefreshToken.builder()
                .account(account)
                .token(refreshToken)
                .build());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {

        // 1. refresh token 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 2. 이메일 추출
        String email = jwtTokenProvider.getNicknameFromToken(refreshToken);

        // 3. DB에 저장된 토큰 정보 조회 및 일치 여부 확인 (탈취 차단 핵심 로직)
        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new JwtInvalidException()); // DB에 토큰이 없으면 잘못된 접근

        if (!email.equals(savedRefreshToken.getAccount().getEmail())) {
            throw new JwtInvalidException(); // DB의 토큰과 클라이언트가 보낸 토큰이 다르면 탈취 의심 처리
        }

        // 4. 검증 완료 후 새로운 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Transactional
    public void logout(String email) {
        accountRepository.findByEmail(email)
                .ifPresent(account -> refreshTokenRepository.deleteByAccountId(account.getId()));
    }
}
