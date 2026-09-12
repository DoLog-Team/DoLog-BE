package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    boolean existsByAccount(Account account);
    Optional<Artist> findByAccountId(UUID accountId);
}
