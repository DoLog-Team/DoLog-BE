package com.dolog.server.domain.account.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.id = :id")
    Optional<Account> findForWithdrawal(@org.springframework.data.repository.query.Param("id") UUID id);

    boolean existsByEmail(String email);
    Optional<Account> findByEmail(String email);
    Optional<Account> findBySocialProviderAndSocialProviderId(String socialProvider, String socialProviderId);
    List<Account> findAllByRole(Role role);
}
