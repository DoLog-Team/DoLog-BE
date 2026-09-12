package com.dolog.server.domain.account.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.account.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.dolog.server.domain.account.web.dto.request.LoginRequest;
import com.dolog.server.domain.account.web.dto.request.RefreshTokenRequest;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "account")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public SuccessResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request.getEmail(), request.getPassword());
        return SuccessResponse.ok(tokens, "로그인 성공");
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
