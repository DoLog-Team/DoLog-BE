package com.dolog.server.global.jwt;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public CustomUserDetails loadUser(UUID accountId, long sessionId) {
        Account account = refreshTokenRepository.findByIdAndAccountId(sessionId, accountId)
                .orElseThrow(JwtInvalidException::new).getAccount();
        account.requireActive();
        return new CustomUserDetails(account.getId(), sessionId, account.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
    }
}
