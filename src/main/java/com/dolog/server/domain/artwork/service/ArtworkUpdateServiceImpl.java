package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkUpdateServiceImpl implements ArtworkUpdateService{
    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final FileService fileService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkArtistService artworkArtistService;

    @Override
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {
        // 1. 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 파일 처리 (S3)
        String mainImgUrl = artwork.getMainImg();
        String locationMapUrl = artwork.getLocationMap();
        try {
            if (request.getMainImageFile() != null && !request.getMainImageFile().isEmpty()) {
                fileService.deleteFile(artwork.getMainImg());
                mainImgUrl = fileService.uploadFile(request.getMainImageFile(), "artworks/main");
            }
            if (request.getLocationMapFile() != null && !request.getLocationMapFile().isEmpty()) {
                fileService.deleteFile(artwork.getLocationMap());
                locationMapUrl = fileService.uploadFile(request.getLocationMapFile(), "artworks/maps");
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 수정 중 오류 발생");
        }

        // 3. 작가 이름 및 프로필 처리
        String currentArtistName = handleArtistProfile(artwork, request);

        // 4. 존(Zone) 처리
        ExhibitionZone zone = null;
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId()).orElse(null);
        }

        // 5. 정보 업데이트
        artwork.updateAllInfo(
                request.getTitle(), request.getDescription(), request.getCategory(), zone,
                request.getMaterial(), request.getSize(), mainImgUrl, locationMapUrl, request.getPurchaseUrl()
        );

        return ArtworkCreateResponse.of(artwork, currentArtistName);
    }

    // 💡 헬퍼 메서드로 분리하면 코드가 더 깔끔해져요!
    private String handleArtistProfile(Artwork artwork, ArtworkUpdateRequest request) {
        if (request.getArtistProfileId() != null) {
            ArtistProfile newProfile = artistProfileRepository.findById(request.getArtistProfileId())
                    .orElseThrow(() -> new RuntimeException("Artist Profile not found"));

            if (!artwork.getArtworkArtistMaps().isEmpty()) {
                artwork.getArtworkArtistMaps().get(0)
                        .updateArtistProfile(newProfile.getArtist(), newProfile, request.getArtistRole());
            }
            return newProfile.getNameKo();
        }

        return artwork.getArtworkArtistMaps().stream()
                .findFirst()
                .map(m -> m.getArtistProfile() != null ? m.getArtistProfile().getNameKo() : m.getArtist().getNameKo())
                .orElse("Unknown Artist");
    }


    @Override
    public ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request) {
        // 1. 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        ExhibitionZone zone = exhibitionZoneRepository.findByIdAndExhibitionId(request.getZoneId(), exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

        // 2. 기본 정보 수정 (UpdateAllInfo 호출)
        artwork.updateAllInfo(
                request.getTitle(), request.getDescription(), request.getCategory(), zone,
                artwork.getMaterial(), artwork.getSize(), artwork.getMainImg(),
                artwork.getLocationMap(), request.getPurchaseUrl()
        );

        // 3. 전문가들에게 위임 (코드가 한 줄씩으로 줄어드는 마법!)
        artworkArtistService.updateArtworkArtists(artwork, request.getArtistIds());
        artworkImageService.updateArtworkImages(artwork, request.getImages());

        // 4. 저장 및 결과 반환
        artworkRepository.saveAndFlush(artwork);

        return ArtworkUpdateFullResponse.of(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getArtworkArtistMaps().stream().map(m -> m.getArtist().getId()).toList(),
                artwork.getArtworkImg().stream().map(ArtworkImg::getId).toList()
        );
    }
}
