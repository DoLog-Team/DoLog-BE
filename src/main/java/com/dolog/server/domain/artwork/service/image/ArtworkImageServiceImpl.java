package com.dolog.server.domain.artwork.service.image;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkImgRepository;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkImageServiceImpl implements ArtworkImageService {

    private final ArtworkImgRepository artworkImgRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFileHandler artworkFileHandler;

    // 작가 본인 작품(두록 어드민은 전체)에만 이미지를 붙인다. 순서를 안 보내면 기존 이미지 뒤에 이어 붙인다.
    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID accountId, boolean isDologAdmin, UUID artworkId,
                                                        List<ArtworkImgCreateRequest> requests) {

        Artwork artwork = artworkValidator.getManagedArtwork(artworkId, accountId, isDologAdmin);

        if (requests == null || requests.isEmpty()
                || requests.stream().anyMatch(req -> req.getImageFile() == null || req.getImageFile().isEmpty())) {
            throw new ArtworkException(ArtworkErrorCode.IMAGE_FILE_REQUIRED);
        }

        int nextOrder = nextOrderOf(artwork);

        List<ArtworkImg> imgs = new ArrayList<>();
        for (ArtworkImgCreateRequest req : requests) {
            Integer orderIndex = req.getOrderIndex() != null ? req.getOrderIndex() : nextOrder++;
            imgs.add(ArtworkImg.builder()
                    .artwork(artwork)
                    .imageUrl(artworkFileHandler.uploadDetailImage(req.getImageFile()))
                    .description(TextUtils.normalizeNewlines(req.getDescription()))
                    .orderIndex(orderIndex)
                    .build());
        }

        artwork.getArtworkImg().addAll(imgs);
        List<UUID> imgIds = artworkImgRepository.saveAll(imgs).stream().map(ArtworkImg::getId).toList();

        return ArtworkImgCreateResponse.from(artworkId, imgIds);
    }

    // 파일을 바꾸면 이전 파일은 커밋된 뒤에 지운다.
    @Override
    public ArtworkImgUpdateResponse updateArtworkImage(UUID accountId, boolean isDologAdmin, UUID artworkId,
                                                       UUID imageId, ArtworkImgUpdateRequest request) {

        Artwork artwork = artworkValidator.getManagedArtwork(artworkId, accountId, isDologAdmin);
        ArtworkImg artworkImg = getImageOf(artwork, imageId);

        String newUrl = artworkFileHandler.uploadDetailImage(request.getImageFile());
        if (newUrl != null) {
            artworkFileHandler.deleteAfterCommit(artworkImg.getImageUrl());
        }

        artworkImg.update(newUrl, TextUtils.normalizeNewlines(request.getDescription()), request.getOrderIndex());
        return new ArtworkImgUpdateResponse(artworkImg.getId());
    }

    @Override
    public void deleteArtworkImage(UUID accountId, boolean isDologAdmin, UUID artworkId, UUID imageId) {

        Artwork artwork = artworkValidator.getManagedArtwork(artworkId, accountId, isDologAdmin);
        ArtworkImg artworkImg = getImageOf(artwork, imageId);

        artwork.getArtworkImg().remove(artworkImg);
        artworkFileHandler.deleteAfterCommit(artworkImg.getImageUrl());
    }

    // 다른 작품의 이미지면 400, 없는 이미지면 404
    private ArtworkImg getImageOf(Artwork artwork, UUID imageId) {

        return artwork.getArtworkImg().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ArtworkException(artworkImgRepository.existsById(imageId)
                        ? ArtworkErrorCode.INVALID_ARTWORK_IMAGE
                        : ArtworkErrorCode.ARTWORK_IMAGE_NOT_FOUND));
    }

    // 통합 수정용: 요청 목록으로 이미지를 맞춘다. 빠진 기존 이미지와 교체된 파일은 커밋된 뒤에 S3 에서 지운다.
    // 기존 이미지는 id 로 유지하고, 새 이미지는 파일로만 받는다 (명세: 유지는 URL, 추가는 imageFile). 순서가 없으면 끝에 붙인다.
    @Override
    public void updateArtworkImages(Artwork artwork, List<ArtworkUpdateFullRequest.ImageUpdateDto> imageDtos) {

        Set<UUID> existingIds = artwork.getArtworkImg().stream().map(ArtworkImg::getId).collect(Collectors.toSet());
        Set<UUID> requestedIds = imageDtos.stream()
                .map(ArtworkUpdateFullRequest.ImageUpdateDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!existingIds.containsAll(requestedIds)) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
        }
        if (imageDtos.stream().anyMatch(dto -> dto.getId() == null
                && (dto.getImageFile() == null || dto.getImageFile().isEmpty()))) {
            throw new ArtworkException(ArtworkErrorCode.IMAGE_FILE_REQUIRED);
        }

        artwork.getArtworkImg().removeIf(img -> {
            if (requestedIds.contains(img.getId())) {
                return false;
            }
            artworkFileHandler.deleteAfterCommit(img.getImageUrl());
            return true;
        });

        for (ArtworkUpdateFullRequest.ImageUpdateDto dto : imageDtos.stream().filter(dto -> dto.getId() != null).toList()) {
            ArtworkImg img = artwork.getArtworkImg().stream()
                    .filter(existing -> existing.getId().equals(dto.getId()))
                    .findFirst()
                    .orElseThrow();
            String uploadedUrl = artworkFileHandler.uploadDetailImage(dto.getImageFile());
            if (uploadedUrl != null) {
                artworkFileHandler.deleteAfterCommit(img.getImageUrl());
            }
            img.update(uploadedUrl, TextUtils.normalizeNewlines(dto.getDescription()), dto.getOrderIndex());
        }

        int nextOrder = nextOrderOf(artwork);
        for (ArtworkUpdateFullRequest.ImageUpdateDto dto : imageDtos.stream().filter(dto -> dto.getId() == null).toList()) {
            artwork.getArtworkImg().add(ArtworkImg.builder()
                    .artwork(artwork)
                    .imageUrl(artworkFileHandler.uploadDetailImage(dto.getImageFile()))
                    .description(TextUtils.normalizeNewlines(dto.getDescription()))
                    .orderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : nextOrder++)
                    .build());
        }
    }

    private int nextOrderOf(Artwork artwork) {
        return artwork.getArtworkImg().stream()
                .map(ArtworkImg::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(0) + 1;
    }
}
