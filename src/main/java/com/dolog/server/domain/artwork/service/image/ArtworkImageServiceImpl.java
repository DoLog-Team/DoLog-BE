package com.dolog.server.domain.artwork.service.image;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.global.util.TextUtils;
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
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    private final FileService fileService; // S3 업로드 로직이 담긴 서비스

    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        List<ArtworkImg> imgs = requests.stream()
                .map(req -> {
                    try {
                        String uploadedUrl = fileService.uploadFile(req.getImageFile(), "artworks/detail");
                        return ArtworkImg.builder()
                                .artwork(artwork)
                                .imageUrl(uploadedUrl)
                                .description(TextUtils.normalizeNewlines(req.getDescription()))
                                .orderIndex(req.getOrderIndex())
                                .build();
                    } catch (IOException e) {
                        throw new ArtworkException(ArtworkErrorCode.FILE_UPLOAD_ERROR);
                    }
                })
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

        String targetUrl = request.getImageUrl();
        try {
            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                targetUrl = fileService.uploadFile(request.getImageFile(), "artworks/detail");
            }
        } catch (IOException e) {
            throw new ArtworkException(ArtworkErrorCode.FILE_UPLOAD_ERROR);
        }

        artworkImg.update(targetUrl, request.getDescription(), request.getOrderIndex());
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