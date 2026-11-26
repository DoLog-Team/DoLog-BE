package com.dolog.server.domain.maintenance.service;

import com.dolog.server.domain.maintenance.entity.MaintenanceMode;
import com.dolog.server.domain.maintenance.exception.MaintenanceErrorCode;
import com.dolog.server.domain.maintenance.exception.MaintenanceException;
import com.dolog.server.domain.maintenance.repository.MaintenanceModeRepository;
import com.dolog.server.domain.maintenance.web.dto.request.MaintenanceModeUpdateRequest;
import com.dolog.server.domain.maintenance.web.dto.response.MaintenanceModeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceModeServiceImpl implements MaintenanceModeService {

    private static final long SINGLETON_ID = 1L;

    private final MaintenanceModeRepository maintenanceModeRepository;

    @Override
    @Transactional(readOnly = true)
    public MaintenanceModeResponse getStatus() {
        return MaintenanceModeResponse.from(getSingleton());
    }

    @Override
    public MaintenanceModeResponse updateStatus(MaintenanceModeUpdateRequest request) {
        MaintenanceMode mode = getSingleton();

        if (Boolean.TRUE.equals(request.getIsActive())) {
            if (request.getStartAt() == null || request.getEndAt() == null) {
                throw new MaintenanceException(MaintenanceErrorCode.PERIOD_REQUIRED);
            }
            if (!request.getEndAt().isAfter(request.getStartAt())) {
                throw new MaintenanceException(MaintenanceErrorCode.INVALID_PERIOD);
            }
            mode.start(request.getStartAt(), request.getEndAt());
        } else {
            mode.end();
        }

        return MaintenanceModeResponse.from(mode);
    }

    private MaintenanceMode getSingleton() {
        return maintenanceModeRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("maintenance_mode row(id=1)가 초기화되지 않았습니다."));
    }
}
