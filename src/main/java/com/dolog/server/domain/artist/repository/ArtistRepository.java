package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {
}
