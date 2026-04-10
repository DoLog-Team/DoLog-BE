package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.host.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.host.HostSnsRequest;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.HostSnsResponse;

import java.util.List;
import java.util.UUID;

public interface ExhibitionHostService {

    ExhibitionHostResponse upsertExhibitionHost(UUID exhibitionId, ExhibitionHostUpsertRequest request) ;

    //SNS
    HostSnsResponse addHostSns(UUID hostId, HostSnsRequest request);
    List<HostSnsResponse> getHostSnsList(UUID hostId);
    List<HostSnsResponse> updateHostSns(UUID snsId, HostSnsRequest request);
    List<HostSnsResponse> deleteHostSns(UUID snsId);
}
