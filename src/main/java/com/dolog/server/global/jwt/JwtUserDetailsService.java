package com.dolog.server.global.jwt;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * JWT 인증용 사용자 정보 로드 서비스
 */
@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다: " + email));

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().name())
        );

        return new CustomUserDetails(
                account.getId(),
                account.getEmail(),
                account.getPassword(),
                authorities
        );
    }
}
