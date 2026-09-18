package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.account.web.dto.response.ExhibitionLoginResponse;
import com.dolog.server.global.jwt.JwtUserDetailsService;
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
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ExhibitionRepository exhibitionRepository;
    private final JwtUserDetailsService userDetailsService;

    @Override
    @Transactional
    public LoginResponse login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidLoginException();
        }
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidLoginException::new);

        if (account.getRole() != Role.DOLOG_ADMIN || account.getPassword() == null
                || !passwordEncoder.matches(password, account.getPassword())) {
            throw new InvalidLoginException();
        }
        return issueTokens(account);
    }

    @Override
    @Transactional
    public ExhibitionLoginResponse loginExhibition(String entryCode) {
        if (entryCode == null || !entryCode.matches("[2-9A-HJ-KM-NP-Za-hj-km-np-z]{8}")) {
            throw new ExhibitionException(ExhibitionErrorCode.ENTRY_CODE_INVALID);
        }
        var exhibition = exhibitionRepository.findByEntryCode(entryCode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ENTRY_CODE_INVALID));
        exhibition.requireEntryCodeValid();
        Account account = exhibition.getAccount();
        if (account.getRole() != Role.EXHIBITION_ADMIN) {
            throw new ExhibitionException(ExhibitionErrorCode.ENTRY_CODE_INVALID);
        }
        var tokens = issueTokens(account);
        // 약관 API 구현 시 실제 동의 이력 조회로 대체한다.
        return new ExhibitionLoginResponse(exhibition.getId(), true, account.getRole(),
                tokens.getAccessToken(), tokens.getRefreshToken());
    }

    @Transactional
    public LoginResponse issueTokens(Account account) {
        userDetailsService.requireAvailable(account);

        // JWT 토큰 발급
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        // 로그인마다 별도 세션을 생성한다.
        RefreshToken session = refreshTokenRepository.save(RefreshToken.builder()
                .account(account)
                .token(refreshToken)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), session.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {

        UUID accountId = jwtTokenProvider.parseRefreshToken(refreshToken);

        // 서버에 남아 있는 세션인지 확인한다.
        RefreshToken savedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(JwtInvalidException::new);

        Account account = savedRefreshToken.getAccount();
        if (!refreshToken.equals(savedRefreshToken.getToken()) || !accountId.equals(account.getId())) {
            throw new JwtInvalidException();
        }
        userDetailsService.requireAvailable(account);

        // 4. 검증 완료 후 새로운 access token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(accountId, savedRefreshToken.getId());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID accountId, long sessionId) {
        refreshTokenRepository.deleteByIdAndAccountId(sessionId, accountId);
    }
}
