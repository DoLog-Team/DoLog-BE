package com.dolog.server.domain.account.service.AccountServiceImpl;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.exception.DuplicateEmailException;
import com.dolog.server.domain.account.exception.InvalidLoginException;
import com.dolog.server.domain.account.exception.InvalidPasswordException;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.service.AccountService;
import com.dolog.server.domain.account.web.dto.request.AdminCreateRequest;
import com.dolog.server.domain.account.web.dto.request.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Account createAdmin(AdminCreateRequest request) {
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException();
        }

        Account admin = Account.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN) // 일반 관리자
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        return accountRepository.save(admin);
    }

    @Override
    public List<Account> getAccounts(Role role) {
        // role 파라미터 없으면 전체 조회
        if (role == null) {
            return accountRepository.findAll();
        }

        // role 있으면 필터 조회
        return accountRepository.findAllByRole(role);
    }


    @Override
    @Transactional
    public Account changePassword(UUID accountId, ChangePasswordRequest request) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다."));

        // 1. 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new InvalidPasswordException();
        }

        // 2. 새 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        // 3. 변경
        account.update(encodedPassword, null, null, null);

        return account; // 🔥 변경된 엔티티 반환
    }
}