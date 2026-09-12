package com.dolog.server.domain.account;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.account.service.AuthService;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.exception.jwt.JwtExpiredException;
import com.dolog.server.global.exception.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class AccountSchemaTests {
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired AuthService auth;
    @Autowired JwtTokenProvider jwt;
    @Autowired JwtUserDetailsService users;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Value("${jwt.secret}") String signingKey;
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

    @Test
    @DisplayName("이메일 없는 계정도 인증·갱신하고 현재 세션만 로그아웃할 수 있다")
    void emailLessAccountCanRefreshAndLogoutOnlyCurrentSession() throws Exception {
        var account = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());
        var first = session(account);
        var second = session(account);
        String access = jwt.createAccessToken(account.getId(), first.getId());
        assertEquals(account.getId().toString(), users.loadUser(account.getId(), first.getId()).getUsername());
        mvc.perform(post("/api/auth/refresh").contextPath("/api").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("refreshToken", first.getToken()))))
                .andExpect(status().isOk());
        assertEquals(account.getId(), jwt.parseAccessToken(auth.refresh(first.getToken()).getAccessToken()).accountId());
        mvc.perform(post("/api/auth/logout").contextPath("/api").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        assertTrue(tokens.findById(first.getId()).isEmpty());
        assertTrue(tokens.findById(second.getId()).isPresent());
        assertThrows(JwtInvalidException.class, () -> auth.refresh(first.getToken()));
        assertNotNull(auth.refresh(second.getToken()).getAccessToken());
        mvc.perform(post("/api/auth/logout").contextPath("/api").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("보호된 GET은 인증과 역할을 검사하고 공개 GET은 토큰 없이 조회할 수 있다")
    void protectedGetChecksAuthenticationAndRole() throws Exception {
        mvc.perform(get("/api/accounts").contextPath("/api"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("JWT_401_UNAUTHORIZED"));
        var admin = auth.login(adminEmail, adminPassword);
        mvc.perform(get("/api/accounts").contextPath("/api").header("Authorization", "Bearer " + admin.getAccessToken()))
                .andExpect(status().isOk());
        var artist = accounts.saveAndFlush(social("KAKAO", "role-check", "role@test.com"));
        var artistSession = session(artist);
        mvc.perform(get("/api/accounts").contextPath("/api").header("Authorization", "Bearer "
                        + jwt.createAccessToken(artist.getId(), artistSession.getId())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GLOBAL_403"));
        mvc.perform(get("/api/v3/api-docs").contextPath("/api")).andExpect(status().isOk());
        mvc.perform(get("/api/accounts").contextPath("/api").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contextPath("/api")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Access와 Refresh Token을 서로 바꿔 사용할 수 없다")
    void tokenTypesCannotBeInterchanged() throws Exception {
        var login = auth.login(adminEmail, adminPassword);
        assertThrows(JwtInvalidException.class, () -> jwt.parseAccessToken(login.getRefreshToken()));
        assertThrows(JwtInvalidException.class, () -> jwt.parseRefreshToken(login.getAccessToken()));
        mvc.perform(get("/api/accounts").contextPath("/api").header("Authorization", "Bearer " + login.getRefreshToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contextPath("/api").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("refreshToken", login.getAccessToken()))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contextPath("/api").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정지·탈퇴 계정은 기존 토큰으로도 인증과 갱신할 수 없다")
    void inactiveAccountsCannotAuthenticateOrRefresh() throws Exception {
        for (AccountStatus status : new AccountStatus[]{AccountStatus.SUSPENDED, AccountStatus.WITHDRAWN}) {
            var account = accounts.saveAndFlush(social("GOOGLE", status.name(), status.name() + "@test.com"));
            var token = session(account);
            String access = jwt.createAccessToken(account.getId(), token.getId());
            account.update(null, null, null, status);
            accounts.flush();
            assertThrows(BaseException.class, () -> auth.refresh(token.getToken()));
            mvc.perform(post("/api/auth/logout").contextPath("/api").header("Authorization", "Bearer " + access))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_403_INACTIVE"));
        }
    }

    @Test
    @DisplayName("토큰의 계정 ID와 DB 세션 소유자가 다르면 인증·갱신·다른 세션 삭제를 거절한다")
    void sessionOwnershipIsChecked() {
        var first = accounts.saveAndFlush(social("KAKAO", "owner-one", "owner-one@test.com"));
        var second = accounts.saveAndFlush(social("KAKAO", "owner-two", "owner-two@test.com"));
        var token = session(first);
        assertThrows(JwtInvalidException.class, () -> users.loadUser(second.getId(), token.getId()));
        auth.logout(second.getId(), token.getId());
        tokens.flush();
        assertTrue(tokens.findById(token.getId()).isPresent());
        var mismatched = tokens.saveAndFlush(RefreshToken.builder().account(second)
                .token(jwt.createRefreshToken(first.getId())).build());
        assertThrows(JwtInvalidException.class, () -> auth.refresh(mismatched.getToken()));
    }

    @Test
    @DisplayName("이메일·비밀번호 로그인은 활성 두록 관리자만 허용한다")
    void passwordLoginIsRestrictedToActiveDologAdmin() {
        var account = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).email("code@test.com")
                .password(encoder.encode("password")).build());
        assertThrows(InvalidLoginException.class, () -> auth.login(account.getEmail(), "password"));
        account.update(null, Role.ARTIST_ADMIN, null, null);
        assertThrows(InvalidLoginException.class, () -> auth.login(account.getEmail(), "password"));
        account.update(null, Role.DOLOG_ADMIN, null, AccountStatus.SUSPENDED);
        assertThrows(BaseException.class, () -> auth.login(account.getEmail(), "password"));
    }

    @Test
    @DisplayName("만료·서명 불일치·기존 이메일 subject·잘못된 세션 ID 토큰을 거절한다")
    void invalidTokenClaimsAreRejected() {
        var accountId = UUID.randomUUID();
        var key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder().subject(accountId.toString()).claim("token_type", "access").claim("sid", 1)
                .expiration(new Date(0)).signWith(key).compact();
        assertThrows(JwtExpiredException.class, () -> jwt.parseAccessToken(expired));
        String old = Jwts.builder().subject(adminEmail).expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key).compact();
        assertThrows(JwtInvalidException.class, () -> jwt.parseAccessToken(old));
        String invalidSession = Jwts.builder().subject(accountId.toString()).claim("token_type", "access").claim("sid", "oops")
                .expiration(new Date(System.currentTimeMillis() + 60000)).signWith(key).compact();
        assertThrows(JwtInvalidException.class, () -> jwt.parseAccessToken(invalidSession));
        String foreign = new JwtTokenProvider("different-test-signing-key-at-least-32-bytes").createAccessToken(accountId, 1);
        assertThrows(JwtInvalidException.class, () -> jwt.parseAccessToken(foreign));
    }

    private RefreshToken session(Account account) {
        return tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
    }

    private Account social(String provider, String subject, String email) {
        return Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE)
                .email(email).socialProvider(provider).socialProviderId(subject).build();
    }
}
