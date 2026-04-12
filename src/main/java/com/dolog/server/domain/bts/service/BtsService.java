package com.dolog.server.domain.bts.service;

import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface BtsService {
    BtsCreateResponse createBts(BtsCreateRequest request) throws IOException;
    BtsCreateResponse updateBts(UUID btsId, BtsUpdateRequest request);
    void deleteBts(UUID btsId);

    List<BtsListResponse> getExhibitionBtsList(UUID exhibitionId);
}
