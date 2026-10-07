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
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    private final FileService fileService; // S3 업로드 로직이 담긴 서비스

    // 작가 본인 작품(두록 어드민은 전체)에만 이미지를 붙인다. 순서를 안 보내면 기존 이미지 뒤에 이어 붙인다.
    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID accountId, boolean isDologAdmin, UUID artworkId,
                                                        List<ArtworkImgCreateRequest> requests) {

        Artwork artwork = artworkValidator.getManagedArtwork(artworkId, accountId, isDologAdmin);

        if (requests == null || requests.isEmpty()
                || requests.stream().anyMatch(req -> req.getImageFile() == null || req.getImageFile().isEmpty())) {
            throw new ArtworkException(ArtworkErrorCode.IMAGE_FILE_REQUIRED);
        }

        int nextOrder = artwork.getArtworkImg().stream()
                .map(ArtworkImg::getOrderIndex)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(0) + 1;

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

        artwork.getArtworkImg().removeIf(img -> {
            if (!requestIds.contains(img.getId().toString())) {
                fileService.deleteFile(img.getImageUrl()); // 💡 S3에서도 삭제 호출
                return true;
            }
            return false;
        });

        // 3. 수정 및 추가
        imageDtos.forEach(imgDto -> {
            String finalUrl; // 람다 내부에서 사용할 '유사 final' 변수

            try {
                if (imgDto.getImageFile() != null && !imgDto.getImageFile().isEmpty()) {
                    // ✅ 해결 1: 인수를 2개로 맞춤 ("artworks/detail" 추가)
                    // ✅ 해결 2: try-catch로 IOException을 처리함
                    finalUrl = fileService.uploadFile(imgDto.getImageFile(), "artworks/detail");
                } else {
                    finalUrl = imgDto.getImageUrl();
                }
            } catch (IOException e) {
                // 💡 람다 내부에서는 체크드 예외를 밖으로 던질 수 없으므로 언체크드 예외로 감싸서 던집니다.
                throw new ArtworkException(ArtworkErrorCode.FILE_UPLOAD_ERROR);
            }

            if (imgDto.getId() != null) {
                artwork.getArtworkImg().stream()
                        .filter(img -> img.getId().equals(imgDto.getId()))
                        .findFirst()
                        .ifPresent(img -> {// 💡 만약 파일이 새로 들어왔다면, 기존 S3 파일 삭제 시도
                            if (imgDto.getImageFile() != null && !imgDto.getImageFile().isEmpty()) {
                                fileService.deleteFile(img.getImageUrl());
                            }
                            img.update(finalUrl, TextUtils.normalizeNewlines(imgDto.getDescription()), imgDto.getOrderIndex());
                        });
            } else {
                if (finalUrl != null) {
                    artwork.getArtworkImg().add(ArtworkImg.builder()
                            .artwork(artwork)
                            .imageUrl(finalUrl)
                            .description(imgDto.getDescription())
                            .orderIndex(imgDto.getOrderIndex())
                            .build());
                }
            }
        });
    }
}