package com.dolog.server.domain.account.repository;

import com.dolog.server.domain.account.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
}
