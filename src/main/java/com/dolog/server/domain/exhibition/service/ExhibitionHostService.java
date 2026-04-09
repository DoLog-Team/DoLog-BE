package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionHostResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ExhibitionHostService {

    public ExhibitionHostResponse upsertExhibitionHost(UUID exhibitionId, ExhibitionHostUpsertRequest request) ;

}
