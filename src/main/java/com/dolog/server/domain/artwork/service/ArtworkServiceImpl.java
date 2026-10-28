package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkCreateProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkDeleteProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkOrderProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkSubmissionProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkVisibilityProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkZoneProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkUpdateProcessor;
import com.dolog.server.domain.artwork.service.artwork.query.ArtworkQueryService;
import com.dolog.server.domain.artwork.service.exhibition.ArtworkExhibitionService;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistMappingRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistRoleRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkSubmitRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkVisibilityUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkHiddenUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkBulkHiddenRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkBulkZoneRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkContentImageResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import org.springframework.web.multipart.MultipartFile;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkSubmitResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkVisibilityUpdateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkHiddenResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkBulkHiddenResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkBulkZoneResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkExhibitionStatusResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkQueryService artworkQueryService;

    private final ArtworkCreateProcessor artworkCreateProcessor;
    private final ArtworkUpdateProcessor artworkUpdateProcessor;
    private final ArtworkDeleteProcessor artworkDeleteProcessor;
    private final ArtworkOrderProcessor artworkOrderProcessor;
    private final ArtworkSubmissionProcessor artworkSubmissionProcessor;
    private final ArtworkVisibilityProcessor artworkVisibilityProcessor;
    private final ArtworkZoneProcessor artworkZoneProcessor;

    private final ArtworkArtistService artworkArtistService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkExhibitionService artworkExhibitionService;


    //============================================================
    // query (조회)
    //============================================================
    @Override
    @Transactional(readOnly = true)
    public Object getArtworks(
            Boolean main,
            String category,
            String search,
            String sort
    ) {

        return artworkQueryService.getArtworks(
                main,
                category,
                search,
                sort
        );
    }

    //============================================================
    // command (생성, 수정, 삭제, 정렬)
    //============================================================
    @Override
    public ArtworkCreateResponse createArtwork(
            UUID accountId,
            ArtworkCreateRequest request
    ) {

        return artworkCreateProcessor.execute(accountId, request);
    }

    @Override
    public ArtworkCreateResponse updateArtwork(
            UUID accountId,
            UUID artworkId,
            ArtworkUpdateRequest request
    ) {

        return artworkUpdateProcessor.update(
                accountId,
                artworkId,
                request
        );
    }

    @Override
    public ArtworkUpdateFullResponse updateArtworkFull(
            UUID exhibitionId,
            UUID artworkId,
            ArtworkUpdateFullRequest request
    ) {

        return artworkUpdateProcessor.updateFull(
                exhibitionId,
                artworkId,
                request
        );
    }

    @Override
    public void deleteArtwork(UUID accountId, UUID artworkId) {

        artworkDeleteProcessor.delete(accountId, artworkId);
    }

    @Override
    public ArtworkSubmitResponse submitArtwork(
            UUID accountId,
            UUID artworkId,
            ArtworkSubmitRequest request
    ) {

        return artworkSubmissionProcessor.submit(accountId, artworkId, request);
    }

    @Override
    public void cancelSubmission(UUID accountId, UUID artworkId) {

        artworkSubmissionProcessor.cancel(accountId, artworkId);
    }

    @Override
    public ArtworkExhibitionStatusResponse getSubmissionStatus(UUID accountId, UUID artworkId) {

        return artworkSubmissionProcessor.getSubmissionStatus(accountId, artworkId);
    }

    @Override
    public ArtworkVisibilityUpdateResponse changeArtworkVisibility(
            UUID accountId,
            UUID artworkId,
            ArtworkVisibilityUpdateRequest request
    ) {

        return artworkVisibilityProcessor.changeVisibility(accountId, artworkId, request.visibility(), request.zoneId());
    }

    @Override
    public ArtworkHiddenResponse changeArtworkHidden(
            UUID accountId,
            UUID artworkId,
            ArtworkHiddenUpdateRequest request
    ) {

        return artworkVisibilityProcessor.changeHidden(accountId, artworkId, request.hidden());
    }

    @Override
    public ArtworkBulkHiddenResponse changeArtworksHidden(
            UUID accountId,
            UUID exhibitionId,
            ArtworkBulkHiddenRequest request
    ) {

        return artworkVisibilityProcessor.changeHiddenBulk(accountId, exhibitionId, request.artworkIds(), request.hidden());
    }

    @Override
    public ArtworkBulkZoneResponse assignArtworksZone(
            UUID accountId,
            UUID exhibitionId,
            ArtworkBulkZoneRequest request
    ) {

        return artworkZoneProcessor.assignZone(accountId, exhibitionId, request.artworkIds(), request.zoneId());
    }

    @Override
    public void reorderArtwork(
            UUID artworkId,
            Integer prev,
            Integer next
    ) {

        artworkOrderProcessor.reorder(
                artworkId,
                prev,
                next
        );
    }

    @Override
    public void moveArtworkZone(
            UUID artworkId,
            UUID zoneId,
            Integer prev,
            Integer next
    ) {

        artworkOrderProcessor.moveZone(
                artworkId,
                zoneId,
                prev,
                next
        );
    }

    //============================================================
    // Artist 매핑
    //============================================================

    @Override
    public ArtworkArtistMappingResponse createArtistMapping(
            UUID accountId,
            UUID artworkId,
            ArtworkArtistMappingRequest request
    ) {

        return artworkArtistService.createArtistMapping(accountId, artworkId, request);
    }

    @Override
    public ArtworkArtistMappingResponse updateArtistMapping(
            UUID accountId,
            UUID artworkId,
            UUID artistId,
            ArtworkArtistRoleRequest request
    ) {

        return artworkArtistService.updateArtistMapping(accountId, artworkId, artistId, request);
    }

    @Override
    public void deleteArtistMapping(
            UUID accountId,
            UUID artworkId,
            UUID artistId
    ) {

        artworkArtistService.deleteArtistMapping(accountId, artworkId, artistId);
    }

    //============================================================
    // Artwork Images
    //============================================================

    @Override
    public ArtworkImgCreateResponse createArtworkImages(
            UUID accountId,
            boolean isDologAdmin,
            UUID artworkId,
            List<ArtworkImgCreateRequest> requests
    ) {

        return artworkImageService.createArtworkImages(
                accountId,
                isDologAdmin,
                artworkId,
                requests
        );
    }

    @Override
    public ArtworkImgUpdateResponse updateArtworkImage(
            UUID accountId,
            boolean isDologAdmin,
            UUID artworkId,
            UUID imageId,
            ArtworkImgUpdateRequest request
    ) {

        return artworkImageService.updateArtworkImage(
                accountId,
                isDologAdmin,
                artworkId,
                imageId,
                request
        );
    }

    @Override
    public void deleteArtworkImage(
            UUID accountId,
            boolean isDologAdmin,
            UUID artworkId,
            UUID imageId
    ) {

        artworkImageService.deleteArtworkImage(
                accountId,
                isDologAdmin,
                artworkId,
                imageId
        );
    }

    @Override
    public ArtworkContentImageResponse uploadContentImage(
            UUID accountId,
            UUID artworkId,
            MultipartFile image
    ) {

        return artworkImageService.uploadContentImage(accountId, artworkId, image);
    }

    //============================================================
    // exhibition
    //============================================================
    @Override
    @Transactional(readOnly = true)
    public ExhibitionArtworkListResponse getExhibitionArtworkList(
            UUID exhibitionId,
            String zone,
            String category,
            String search
    ) {

        return artworkExhibitionService.getExhibitionArtworkList(
                exhibitionId,
                zone,
                category,
                search
        );
    }
}