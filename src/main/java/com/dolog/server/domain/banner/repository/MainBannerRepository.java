package com.dolog.server.domain.banner.repository;

import com.dolog.server.domain.banner.entity.MainBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MainBannerRepository extends JpaRepository<MainBanner, Long> {
    List<MainBanner> findAllByIsVisibleTrueOrderByOrderIndexAsc();
}
