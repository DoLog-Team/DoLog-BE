package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID> {
}
