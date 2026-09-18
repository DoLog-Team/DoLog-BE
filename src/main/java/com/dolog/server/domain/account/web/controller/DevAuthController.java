package com.dolog.server.domain.account.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.service.AuthServiceImpl;
import com.dolog.server.domain.account.web.dto.response.LoginResponse;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Profile("(local | dev) & !prod")
@RequestMapping("/auth/dev")
@SecurityRequirement(name = "bearerAuth")
public class DevAuthController {
    private final AccountRepository accounts;
    private final ArtistRepository artists;
    private final AuthServiceImpl auth;

    public enum Fixture { ARTIST_1, ARTIST_2 }
    public record LoginRequest(@NotNull Fixture fixture) { }

    @Operation(summary = "개발용 작가 로그인", description = "DOLOG_ADMIN만 고정 테스트 작가의 세션을 발급할 수 있습니다.")
    @PostMapping("/artist-login")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @Transactional
    public SuccessResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String identity = request.fixture().name();
        Account account = accounts.findBySocialProviderAndSocialProviderId("DEV", identity)
                .orElseGet(() -> accounts.save(Account.builder()
                        .email(identity.toLowerCase(java.util.Locale.ROOT) + "@example.test")
                        .socialProvider("DEV").socialProviderId(identity)
                        .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build()));
        if (account.getRole() != Role.ARTIST_ADMIN) throw new InvalidLoginException();
        account.requireActive();
        // 기존 작가를 가져오거나 재연결하지 않고 개발용 계정에만 작가를 생성한다.
        if (artists.findByAccountId(account.getId()).isEmpty()) {
            artists.save(Artist.builder().account(account).nameKo("개발용 작가 " + identity).build());
        }
        return SuccessResponse.ok(auth.issueTokens(account), "개발용 작가 로그인 성공");
    }
}
