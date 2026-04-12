package com.dolog.server.domain.bts.service;

import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;

import java.io.IOException;
import java.util.UUID;

public interface BtsService {
    public BtsCreateResponse createBts(BtsCreateRequest request) throws IOException;
}
