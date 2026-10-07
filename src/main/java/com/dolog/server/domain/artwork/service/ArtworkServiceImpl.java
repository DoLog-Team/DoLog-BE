package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkCreateProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkDeleteProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkOrderProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkSubmissionProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkVisibilityProcessor;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkUpdateProcessor;
import com.dolog.server.domain.artwork.service.artwork.query.ArtworkDetailQueryService;
import com.dolog.server.domain.artwork.service.artwork.query.ArtworkQueryService;
import com.dolog.server.domain.artwork.service.exhibition.ArtworkExhibitionService;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistMappingRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkSubmitRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkStatusUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkHiddenUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkSubmitResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkStatusResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkHiddenResponse;
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
    private final ArtworkDetailQueryService artworkDetailQueryService;

    private final ArtworkCreateProcessor artworkCreateProcessor;
    private final ArtworkUpdateProcessor artworkUpdateProcessor;
    private final ArtworkDeleteProcessor artworkDeleteProcessor;
    private final ArtworkOrderProcessor artworkOrderProcessor;
    private final ArtworkSubmissionProcessor artworkSubmissionProcessor;
    private final ArtworkVisibilityProcessor artworkVisibilityProcessor;

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

    @Override
    @Transactional(readOnly = true)
    public ArtworkDetailResponse getArtworkDetail(
            UUID exhibitionId,
            UUID artworkId
    ) {

        return artworkDetailQueryService.getArtworkDetail(
                exhibitionId,
                artworkId
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
    public ArtworkStatusResponse changeArtworkStatus(
            UUID accountId,
            UUID artworkId,
            ArtworkStatusUpdateRequest request
    ) {

        return artworkVisibilityProcessor.changeStatus(accountId, artworkId, request.status());
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
            UUID artworkId,
            ArtworkArtistMappingRequest request
    ) {

        return artworkArtistService.createArtistMapping(
                artworkId,
                request
        );
    }

    @Override
    public ArtworkArtistMappingResponse updateArtistMapping(
            UUID artworkId,
            UUID artistProfileId,
            ArtworkArtistMappingRequest request
    ) {

        return artworkArtistService.updateArtistMapping(
                artworkId,
                artistProfileId,
                request
        );
    }

    @Override
    public void deleteArtistMapping(
            UUID artworkId,
            UUID artistProfileId
    ) {

        artworkArtistService.deleteArtistMapping(
                artworkId,
                artistProfileId
        );
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