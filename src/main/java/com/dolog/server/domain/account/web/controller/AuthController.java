package com.dolog.server.domain.account.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.account.service.AuthService;
import com.dolog.server.domain.account.service.OAuthCodeClient;
import com.dolog.server.domain.account.entity.enums.SocialProvider;
import com.dolog.server.domain.account.exception.SocialLoginErrorCode;
import com.dolog.server.domain.account.web.dto.request.SocialLoginRequest;
import com.dolog.server.domain.account.web.dto.response.SocialLoginResponse;
import com.dolog.server.global.exception.BaseException;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.dolog.server.domain.account.web.dto.request.LoginRequest;
import com.dolog.server.domain.account.web.dto.request.ExhibitionLoginRequest;
import com.dolog.server.domain.account.web.dto.response.ExhibitionLoginResponse;
import com.dolog.server.domain.account.web.dto.request.RefreshTokenRequest;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "account")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Map<SocialProvider, OAuthCodeClient> socialClients;

    public AuthController(AuthService authService, List<OAuthCodeClient> socialClients) {
        this.authService = authService;
        this.socialClients = socialClients.stream()
                .collect(Collectors.toUnmodifiableMap(OAuthCodeClient::provider, Function.identity()));
    }

    // 로그인
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public SuccessResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request.getEmail(), request.getPassword());
        return SuccessResponse.ok(tokens, "로그인 성공");
    }


    @Operation(summary = "전시 어드민 코드 로그인")
    @PostMapping("/exhibition/login")
    public SuccessResponse<ExhibitionLoginResponse> loginExhibition(@Valid @RequestBody ExhibitionLoginRequest request) {
        return SuccessResponse.ok(authService.loginExhibition(request.getEntryCode()), "전시 어드민 로그인 성공");
    }

    @Operation(summary = "소셜 로그인/가입")
    @PostMapping("/social/login")
    public SuccessResponse<SocialLoginResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        var client = socialClients.get(request.provider());
        if (client == null) {
            throw new BaseException(SocialLoginErrorCode.PROVIDER_NOT_SUPPORTED);
        }
        var profile = client.fetchProfile(request.authorizationCode(), request.redirectUri());
        return SuccessResponse.ok(authService.socialLogin(request.provider(), profile), "소셜 로그인 성공");
    }

    // 토큰 재발급
    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public SuccessResponse<TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        TokenResponse response = authService.refresh(request.getRefreshToken());

        return SuccessResponse.ok(
                response,
                "토큰 재발급 성공"
        );
    }

    @Operation(summary = "현재 로그인 세션 로그아웃")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public SuccessResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails user) {
        authService.logout(user.getId(), user.getSessionId());
        return SuccessResponse.ok(null, "로그아웃 성공");
    }
}
