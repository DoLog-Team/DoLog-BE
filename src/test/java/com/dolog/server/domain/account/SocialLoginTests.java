package com.dolog.server.domain.account;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.TermsAgreement;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.entity.enums.SocialProvider;
import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.account.repository.TermsAgreementRepository;
import com.dolog.server.domain.account.service.AuthServiceImpl;
import com.dolog.server.domain.account.service.KakaoClient;
import com.dolog.server.domain.account.service.SocialProfile;
import com.dolog.server.domain.account.service.TermsAgreementService;
import com.dolog.server.domain.account.web.dto.request.TermsAgreementRequest;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.jwt.JwtUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// 카카오 통신·소셜 계정 처리·약관 동의 단위 테스트. Spring 컨텍스트 없이 실행한다.
class SocialLoginTests {

    @Test
    @DisplayName("카카오 인가 코드를 교환하고 검증된 이메일만 프로필에 담는다.")
    void kakaoExchangesCodeAndAcceptsOnlyVerifiedEmail() {
        var http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        var client = new KakaoClient(http, "rest-key", "server-secret", "http://localhost:3000/oauth/callback/kakao");
        String redirectUri = "http://localhost:3000/oauth/callback/kakao";

        var rejected = assertThrows(BaseException.class,
                () -> client.fetchProfile("code", "http://localhost:3000/oauth/callback/kakao/evil"));
        assertEquals(SocialLoginErrorCode.INVALID_REDIRECT_URI, rejected.getErrorCode());

        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString(
                        "redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Foauth%2Fcallback%2Fkakao")))
                .andRespond(withSuccess("{\"access_token\":\"provider-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(header("Authorization", "Bearer provider-token"))
                .andRespond(withSuccess("{\"id\":12345,\"kakao_account\":{\"email\":\"x@example.com\","
                        + "\"is_email_valid\":true,\"is_email_verified\":false,"
                        + "\"profile\":{\"nickname\":\"작가\"}}}", MediaType.APPLICATION_JSON));

        var profile = client.fetchProfile("code", redirectUri);
        assertEquals("12345", profile.providerId());
        assertEquals("작가", profile.name());
        assertNull(profile.email());
        server.verify();

        server.reset();
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest().body("{\"error\":\"invalid_grant\",\"error_code\":\"KOE320\"}"));
        var invalidCode = assertThrows(BaseException.class, () -> client.fetchProfile("used-code", redirectUri));
        assertEquals(SocialLoginErrorCode.INVALID_AUTHORIZATION_CODE, invalidCode.getErrorCode());
        server.verify();
    }

    @Test
    @DisplayName("최초 소셜 로그인은 ARTIST_ADMIN 계정을 만들고 기존 토큰 발급을 재사용한다.")
    void socialLoginCreatesAccountAndReusesTokenIssuance() {
        var accountRepo = mock(AccountRepository.class);
        var tokenRepo = mock(RefreshTokenRepository.class);
        var jwtProvider = mock(JwtTokenProvider.class);
        var terms = mock(TermsAgreementService.class);
        var accountId = UUID.randomUUID();
        var account = Account.builder().id(accountId).email("verified@example.com")
                .socialProvider("KAKAO").socialProviderId("12345")
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        when(accountRepo.findBySocialProviderAndSocialProviderId("KAKAO", "12345")).thenReturn(Optional.empty());
        when(accountRepo.saveAndFlush(any(Account.class))).thenReturn(account);
        when(terms.needsAgreement(accountId)).thenReturn(true);
        when(jwtProvider.createRefreshToken(accountId)).thenReturn("refresh");
        when(tokenRepo.save(any(RefreshToken.class))).thenAnswer(call -> {
            var session = call.getArgument(0, RefreshToken.class);
            ReflectionTestUtils.setField(session, "id", 1L);
            return session;
        });
        when(jwtProvider.createAccessToken(accountId, 1L)).thenReturn("access");
        var auth = new AuthServiceImpl(accountRepo, tokenRepo, mock(PasswordEncoder.class),
                jwtProvider, mock(ExhibitionRepository.class), mock(JwtUserDetailsService.class), terms);

        var result = auth.socialLogin(SocialProvider.KAKAO, new SocialProfile("12345", null, "verified@example.com"));
        assertTrue(result.isFirstLogin());
        assertTrue(result.needsTermsAgreement());
        assertNull(result.profile().name());
        assertEquals("verified@example.com", result.profile().email());
        assertEquals(Role.ARTIST_ADMIN, result.role());
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertTrue(new ObjectMapper().valueToTree(result).path("isFirstLogin").asBoolean());
        verify(accountRepo).saveAndFlush(argThat(created -> created.getPassword() == null
                && created.getRole() == Role.ARTIST_ADMIN && created.getAccountStatus() == AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("약관 동의는 현재 버전과 필수 항목 전부를 요구한다.")
    void termsAgreementRequiresCurrentVersionAndAllRequiredConsents() {
        var accountRepo = mock(AccountRepository.class);
        var agreements = mock(TermsAgreementRepository.class);
        var service = new TermsAgreementService(agreements, accountRepo, "v1.0");
        var accountId = UUID.randomUUID();
        var account = Account.builder().id(accountId).role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build();
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(agreements.hasRequiredAgreement(accountId, "v1.0")).thenReturn(false, true);
        assertTrue(service.needsAgreement(accountId));
        assertThrows(BaseException.class, () -> service.agree(accountId,
                new TermsAgreementRequest("v1.0", false, true, true, false, false, false)));
        assertThrows(BaseException.class, () -> service.agree(accountId,
                new TermsAgreementRequest("v1.0", true, true, false, false, false, false)));
        assertThrows(BaseException.class, () -> service.agree(accountId,
                new TermsAgreementRequest("v1.0", true, true, true, false, false, true)));
        assertThrows(BaseException.class, () -> service.agree(accountId,
                new TermsAgreementRequest("old", true, true, true, false, false, false)));
        verify(agreements, never()).save(any(TermsAgreement.class));

        service.agree(accountId, new TermsAgreementRequest("v1.0", true, true, true,
                false, true, true));
        verify(agreements).save(argThat(saved -> saved.getAccount() == account
                && saved.isAge14OrOverConfirmed() && saved.isServiceTermsAgreed() && saved.isPrivacyAgreed()
                && !saved.isPrivacy3rdPartyAgreed() && Boolean.TRUE.equals(saved.getAdEmailAgreed())
                && Boolean.TRUE.equals(saved.getAdKakaoAgreed()) && Boolean.TRUE.equals(saved.getAdSmsAgreed())
                && "v1.0".equals(saved.getTermsVersion())));
        assertFalse(service.needsAgreement(accountId));
    }
}
