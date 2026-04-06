package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArtworkImgRepository extends JpaRepository<ArtworkImg, UUID> {
}
