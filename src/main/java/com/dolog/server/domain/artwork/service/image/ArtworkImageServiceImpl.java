package com.dolog.server.domain.artwork.service.image;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkImgRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkImageServiceImpl implements ArtworkImageService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkImgRepository artworkImgRepository;

    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        List<ArtworkImg> imgs = requests.stream()
                .map(req -> ArtworkImg.builder()
                        .artwork(artwork)
                        .imageUrl(req.getImageUrl())
                        .description(req.getDescription())
                        .orderIndex(req.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        List<ArtworkImg> savedImgs = artworkImgRepository.saveAll(imgs);
        List<UUID> imgIds = savedImgs.stream().map(ArtworkImg::getId).collect(Collectors.toList());

        return ArtworkImgCreateResponse.from(artworkId, imgIds);
    }

    @Override
    public ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        ArtworkImg artworkImg = artworkImgRepository.findById(imageId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_IMAGE_NOT_FOUND));

        if (!artworkImg.getArtwork().getId().equals(artwork.getId())) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
        }

        artworkImg.update(request.getImageUrl(), request.getDescription(), request.getOrderIndex());
        return new ArtworkImgUpdateResponse(artworkImg.getId());
    }

    @Override
    public void deleteArtworkImage(UUID artworkId, UUID imageId) {
        ArtworkImg artworkImg = artworkImgRepository.findById(imageId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_IMAGE_NOT_FOUND));

        if (!artworkImg.getArtwork().getId().equals(artworkId)) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
        }

        artworkImgRepository.delete(artworkImg);
    }

    @Override
    @Transactional
    public void updateArtworkImages(Artwork artwork, List<ArtworkUpdateFullRequest.ImageUpdateDto> imageDtos) {
        // 1. 검증: 이 작품에 속한 이미지가 맞는지 체크
        List<UUID> existingImgIds = artwork.getArtworkImg().stream()
                .map(ArtworkImg::getId)
                .toList();

        imageDtos.stream()
                .map(ArtworkUpdateFullRequest.ImageUpdateDto::getId)
                .filter(Objects::nonNull)
                .forEach(id -> {
                    if (!existingImgIds.contains(id)) {
                        throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
                    }
                });

        // 2. 삭제: 요청 목록에 없는 기존 이미지 제거
        Set<String> requestIds = imageDtos.stream()
                .map(img -> img.getId() != null ? img.getId().toString() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        artwork.getArtworkImg().removeIf(img -> !requestIds.contains(img.getId().toString()));

        // 3. 수정 및 추가
        imageDtos.forEach(imgDto -> {
            if (imgDto.getId() != null) {
                // 기존 이미지 내용 업데이트
                artwork.getArtworkImg().stream()
                        .filter(img -> img.getId().toString().equals(imgDto.getId().toString()))
                        .findFirst()
                        .ifPresent(img -> img.update(imgDto.getImageUrl(), imgDto.getDescription(), imgDto.getOrderIndex()));
            } else {
                // 새 이미지 추가
                artwork.getArtworkImg().add(ArtworkImg.builder()
                        .artwork(artwork)
                        .imageUrl(imgDto.getImageUrl())
                        .description(imgDto.getDescription())
                        .orderIndex(imgDto.getOrderIndex())
                        .build());
            }
        });
    }
}