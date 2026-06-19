package com.dolog.server.domain.bts.service;

import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsMappingUpdateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsDetailResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsMappingUpdateResponse;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface BtsService {
    BtsResponse createBts(BtsCreateRequest request, MultipartFile mainImg) throws IOException;
    BtsResponse updateBts(UUID exhibitionId, UUID btsId, BtsUpdateRequest request, MultipartFile mainImg) throws IOException;
    void deleteBts(UUID exhibitionId, UUID btsId);
    List<BtsListResponse> getExhibitionBtsList(UUID exhibitionId);
    BtsMappingUpdateResponse syncBtsMapping(UUID exhibitionId, UUID btsId, BtsMappingUpdateRequest request);
    BtsDetailResponse getBtsDetail(UUID exhibitionId, UUID btsId);
}
