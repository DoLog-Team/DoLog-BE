package com.dolog.server.domain.artwork.service.artist;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistMappingRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ArtworkArtistService {
    ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request);
    ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistProfileId, ArtworkArtistMappingRequest request);
    void deleteArtistMapping(UUID artworkId, UUID artistProfileId);
    void updateArtworkArtists(Artwork artwork, List<UUID> artistIds, Map<UUID, String> artistRoles);

    // N+1 방지를 위한 작가명 일괄 조회 (Map 반환)
    Map<UUID, String> fetchArtistMap(List<UUID> artworkIds);
}
