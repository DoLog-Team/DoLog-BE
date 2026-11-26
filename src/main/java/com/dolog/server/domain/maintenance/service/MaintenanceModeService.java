package com.dolog.server.domain.maintenance.service;

import com.dolog.server.domain.maintenance.web.dto.request.MaintenanceModeUpdateRequest;
import com.dolog.server.domain.maintenance.web.dto.response.MaintenanceModeResponse;

public interface MaintenanceModeService {

    MaintenanceModeResponse getStatus();

    MaintenanceModeResponse updateStatus(MaintenanceModeUpdateRequest request);
}
