package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    @Transactional
    public ArtworkCreateResponse createArtwork(ArtworkCreateRequest request) {
        // 1. 전시회 조회
        Exhibition exhibition = exhibitionRepository.findById(request.getExhibitionId())
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. 전시 구역(Zone) 조회 및 검증
        ExhibitionZone zone = null;
        if (request.getZoneId() != null) {
            // TODO: ZONE_NOT_FOUND 에러 코드 추가 시 교체 필요
            zone = exhibitionZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

            // TODO: 해당 구역이 요청된 전시회의 구역이 맞는지 검증하는 로직 및 에러 코드(INVALID_ZONE) 추가 필요
            /*
            if (!zone.getExhibition().getId().equals(exhibition.getId())) {
                throw new ExhibitionException(ExhibitionErrorCode.INVALID_ZONE_FOR_EXHIBITION);
            }
            */
        }

        // 3. Artwork 엔티티 생성 및 저장
        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .exhibitionZone(zone)
                .title(request.getTitle())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(request.getDescription())
                .mainImg(request.getMainImage())
                .purchaseUrl(request.getPurchaseUrl())
                // TODO: Artwork 엔티티에 orderIndex 필드 추가 후 아래 주석 해제
                // .orderIndex(request.getOrderIndex())
                .build();

        Artwork savedArtwork = artworkRepository.save(artwork);

        return ArtworkCreateResponse.from(savedArtwork.getId());
    }
}