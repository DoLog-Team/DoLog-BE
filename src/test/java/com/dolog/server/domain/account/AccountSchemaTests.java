package com.dolog.server.domain.account;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.account.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AccountSchemaTests {
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired AuthService auth;
    @Value("${admin.email}") String adminEmail;
    @Value("${admin.password}") String adminPassword;

    @Test
    @DisplayName("소셜 사용자 ID는 제공자별로 대소문자를 구분하고 중복을 허용하지 않음.")
    void socialIdentityIsUniqueWithinProvider() {
        accounts.saveAndFlush(social("KAKAO", "subject", "first@test.com"));
        accounts.saveAndFlush(social("GOOGLE", "subject", "second@test.com"));
        accounts.saveAndFlush(social("KAKAO", "SUBJECT", "case@test.com"));

        assertEquals("first@test.com", accounts.findBySocialProviderAndSocialProviderId("KAKAO", "subject")
                .orElseThrow().getEmail());
        assertThrows(DataIntegrityViolationException.class,
                () -> accounts.saveAndFlush(social("KAKAO", "subject", "duplicate@test.com")));
    }

    @Test
    @DisplayName("이메일 값이 있으면 중복 저장을 허용하지 않는다")
    void duplicateNonNullEmailsAreRejected() {
        accounts.saveAndFlush(social("KAKAO", "one", "same@test.com"));
        assertThrows(DataIntegrityViolationException.class,
                () -> accounts.saveAndFlush(social("GOOGLE", "two", "same@test.com")));
    }

    @Test
    @DisplayName("비밀번호 없는 소셜 계정이나 이메일 없는 요청의 비밀번호 로그인을 거절한다")
    void passwordLoginRejectsSocialAccountAndMissingEmail() {
        accounts.saveAndFlush(social("GOOGLE", "social", "social@test.com"));
        assertThrows(InvalidLoginException.class, () -> auth.login("social@test.com", "password"));
        assertThrows(InvalidLoginException.class, () -> auth.login(null, "password"));
    }

    @Test
    @DisplayName("두 번 로그인해도 두 Refresh Token을 모두 사용할 수 있다")
    void twoLoginsKeepBothRefreshTokensUsable() {
        var first = auth.login(adminEmail, adminPassword);
        var second = auth.login(adminEmail, adminPassword);
        assertNotEquals(first.getRefreshToken(), second.getRefreshToken());
        tokens.flush();
        assertNotNull(auth.refresh(first.getRefreshToken()).getAccessToken());
        assertNotNull(auth.refresh(second.getRefreshToken()).getAccessToken());
        assertEquals(Role.DOLOG_ADMIN, accounts.findByEmail(adminEmail).orElseThrow().getRole());
    }

    private Account social(String provider, String subject, String email) {
        return Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE)
                .email(email).socialProvider(provider).socialProviderId(subject).build();
    }
}
