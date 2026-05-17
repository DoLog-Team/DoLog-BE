package com.dolog.server.domain.artwork.support.file;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ArtworkFileHandler {

    private final FileService fileService;

    /**
     * 작품 생성 파일 업로드
     */
    public ArtworkFileUrls uploadArtworkFiles(
            ArtworkCreateRequest request
    ) {

        try {

            String mainImgUrl = uploadMainImage(
                    request.getMainImageFile()
            );

            String locationMapUrl = uploadLocationMap(
                    request.getLocationMapFile()
            );

            return new ArtworkFileUrls(
                    mainImgUrl,
                    locationMapUrl
            );

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패");
        }
    }

    /**
     * 작품 수정 파일 업로드
     */
    public ArtworkFileUrls updateArtworkFiles(
            Artwork artwork,
            ArtworkUpdateRequest request
    ) {

        try {

            String mainImgUrl = artwork.getMainImg();
            String locationMapUrl = artwork.getLocationMap();

            // 메인 이미지 수정
            if (request.getMainImageFile() != null
                    && !request.getMainImageFile().isEmpty()) {

                deleteIfExists(artwork.getMainImg());

                mainImgUrl = fileService.uploadFile(
                        request.getMainImageFile(),
                        "artworks/main"
                );
            }

            // 위치 지도 수정
            if (request.getLocationMapFile() != null
                    && !request.getLocationMapFile().isEmpty()) {

                deleteIfExists(artwork.getLocationMap());

                locationMapUrl = fileService.uploadFile(
                        request.getLocationMapFile(),
                        "artworks/maps"
                );
            }

            return new ArtworkFileUrls(
                    mainImgUrl,
                    locationMapUrl
            );

        } catch (IOException e) {
            throw new RuntimeException("파일 수정 실패");
        }
    }

    // =========================
    // Private Methods
    // =========================

    private String uploadMainImage(
            org.springframework.web.multipart.MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        return fileService.uploadFile(
                file,
                "artworks/main"
        );
    }

    private String uploadLocationMap(
            org.springframework.web.multipart.MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        return fileService.uploadFile(
                file,
                "artworks/maps"
        );
    }

    private void deleteIfExists(
            String fileUrl
    ) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        fileService.deleteFile(fileUrl);
    }
}