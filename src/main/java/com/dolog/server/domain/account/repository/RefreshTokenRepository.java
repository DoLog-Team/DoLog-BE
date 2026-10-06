package com.dolog.server.domain.account.repository;

import com.dolog.server.domain.account.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByIdAndAccountId(Long id, UUID accountId);
    void deleteByIdAndAccountId(Long id, UUID accountId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("delete from RefreshToken t where t.account.id = :id")
    void deleteAllByAccountId(@org.springframework.data.repository.query.Param("id") UUID id);

}
