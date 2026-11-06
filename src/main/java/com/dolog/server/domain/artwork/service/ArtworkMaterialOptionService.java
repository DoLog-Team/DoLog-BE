package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionReorderRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkMaterialOptionRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkMaterialOptionResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkMaterialOptionService {

    List<ArtworkMaterialOptionResponse> getOptions();

    void createOption(ArtworkMaterialOptionRequest request);

    void updateOption(UUID materialOptionId, ArtworkMaterialOptionRequest request);

    void deleteOption(UUID materialOptionId);

    void reorderOptions(List<ArtworkMaterialOptionReorderRequest> requests);
}
