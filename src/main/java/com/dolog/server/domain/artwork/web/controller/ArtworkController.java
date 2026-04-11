package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.ArtworkService;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;

    // 1. 작품 전체 목록 조회
    @GetMapping("/artworks")
    public SuccessResponse<Object> getArtworks(
            @RequestParam(required = false) Boolean main,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        Object data = artworkService.getArtworks(main, category, search);
        return SuccessResponse.ok(data);
    }

    // 2. 작품 기본 정보 등록
    @PostMapping("/exhibitions/{exhibitionId}/artworks")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkCreateResponse> createArtwork(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ArtworkCreateRequest request) {
        ArtworkCreateResponse data = artworkService.createArtwork(exhibitionId, request);
        return SuccessResponse.created(data);
    }

    // 3. 작품 기본 정보 수정 (PATCH)
    @PatchMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkCreateResponse> updateArtwork(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkUpdateRequest request) {
        ArtworkCreateResponse data = artworkService.updateArtwork(exhibitionId, artworkId, request);
        return SuccessResponse.ok(data, "정보가 성공적으로 수정되었습니다.");
    }

    // 4. 작품 삭제 (DELETE)
    @DeleteMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteArtwork(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId) {
        artworkService.deleteArtwork(exhibitionId, artworkId);
        return SuccessResponse.ok(null, "작품이 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 상세 이미지 관련 API ] ---------------- */

    // 5. 작품 상세 이미지 리스트 등록 (POST)
    @PostMapping("/artworks/{artworkId}/images")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkImgCreateResponse> createArtworkImages(
            @PathVariable UUID artworkId,
            @RequestBody List<ArtworkImgCreateRequest> requests
    ) {
        ArtworkImgCreateResponse response = artworkService.createArtworkImages(artworkId, requests);
        return SuccessResponse.ok(response, "작품 상세 이미지 등록에 성공하였습니다.");
    }

    // 6. 작품 상세 이미지 개별 수정 (PATCH)
    @PatchMapping("/artworks/{artworkId}/images/{imageId}") // 주소 확인! images 입니다.
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkImgUpdateResponse> updateArtworkImage(
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId,
            @RequestBody ArtworkImgUpdateRequest request) {
        ArtworkImgUpdateResponse data = artworkService.updateArtworkImage(artworkId, imageId, request);
        return SuccessResponse.ok(data, "상세 이미지 정보가 성공적으로 수정되었습니다.");
    }

    // 7. 작품 상세 이미지 개별 삭제 (DELETE)
    @DeleteMapping("/artworks/{artworkId}/images/{imageId}") // 주소 확인! images 입니다.
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteArtworkImage(
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId) {
        artworkService.deleteArtworkImage(artworkId, imageId);
        return SuccessResponse.ok(null, "상세 이미지가 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 작가 매핑 관련 API ] ---------------- */

    // 8. 작품 작가 매핑 등록 (POST)
    @PostMapping("/artworks/{artworkId}/artists")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkArtistMappingResponse> createArtistMapping(
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.createArtistMapping(artworkId, request);
        return SuccessResponse.created(data);
    }

    // 9. 작품 작가 매핑 수정 (PATCH)
    @PatchMapping("/artworks/{artworkId}/artists/{artistId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkArtistMappingResponse> updateArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistId,
            @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.updateArtistMapping(artworkId, artistId, request);
        return SuccessResponse.ok(data, "작가 역할이 성공적으로 수정되었습니다.");
    }

    // 10. 작품 작가 매핑 삭제 (DELETE)
    @DeleteMapping("/artworks/{artworkId}/artists/{artistId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistId) {
        artworkService.deleteArtistMapping(artworkId, artistId);
        return SuccessResponse.ok(null, "작가 연결이 성공적으로 해제되었습니다.");
    }
}