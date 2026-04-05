package com.dolog.server.domain.account.web.controller;

import com.dolog.server.domain.account.service.AuthService;
import com.dolog.server.domain.account.web.dto.request.ChangePasswordRequest;
import com.dolog.server.domain.account.web.dto.request.LoginRequest;
import com.dolog.server.domain.account.web.dto.request.RefreshTokenRequest;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.account.web.dto.response.TokenResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public SuccessResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request.getEmail(), request.getPassword());
        return SuccessResponse.ok(tokens, "로그인 성공");
    }


    // 토큰 재발급
    @PostMapping("/refresh")
    public SuccessResponse<TokenResponse> refresh(
            @RequestBody RefreshTokenRequest request
    ) {
        TokenResponse response = authService.refresh(request.getRefreshToken());

        return SuccessResponse.ok(
                response,
                "토큰 재발급 성공"
        );
    }
}
