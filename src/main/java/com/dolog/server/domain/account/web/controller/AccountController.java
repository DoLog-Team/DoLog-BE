package com.dolog.server.domain.account.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.service.AccountService;
import com.dolog.server.domain.account.web.dto.request.AdminCreateRequest;
import com.dolog.server.domain.account.web.dto.request.ChangePasswordRequest;
import com.dolog.server.domain.account.web.dto.response.AccountResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "account")
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService; // 인터페이스 타입으로 DI

    // 슈퍼어드민만 접근 가능\
    // admin 계정 생성
    @Operation(summary = "admin 계정(전시 총대) 생성")
    @PostMapping("/admin")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<AccountResponse> createAdmin(
            @RequestBody AdminCreateRequest request
    ) {
        Account admin = accountService.createAdmin(request);

        AccountResponse response = AccountResponse.from(admin);

        return SuccessResponse.ok(
                response,
                "관리자 생성 완료"
        );
    }

    // admin 계정 조회
    @Operation(summary = "계정 목록 조회")
    @GetMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<List<AccountResponse>> getAccounts(
            @RequestParam(required = false) Role role
    ) {

        List<AccountResponse> data = accountService.getAccounts(role)
                .stream()
                .map(AccountResponse::from)
                .toList();

        return SuccessResponse.ok(
                data,
                "계정 목록 조회 성공"
        );
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/me/password")
    public SuccessResponse<AccountResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequest request
    ) {

        Account account = accountService.changePassword(userDetails.getId(), request);

        AccountResponse response = AccountResponse.from(account);

        return SuccessResponse.ok(
                response,
                "비밀번호 변경 성공"
        );
    }
}