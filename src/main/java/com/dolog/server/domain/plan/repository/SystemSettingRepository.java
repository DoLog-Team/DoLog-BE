package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
