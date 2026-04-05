package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(String email, String password) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidLoginException::new);

        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new InvalidLoginException();
        }

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(account.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    public TokenResponse refresh(String refreshToken) {

        // 1. refresh token 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 2. 이메일 추출
        String email = jwtTokenProvider.getNicknameFromToken(refreshToken);

        // 3. 새로운 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }
}