package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.artwork.ArtworkDetailService;
import com.dolog.server.domain.artwork.service.artwork.ArtworkService;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import com.dolog.server.global.response.SuccessResponse;
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
    private final ArtworkDetailService artworkDetailService;

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
    @PostMapping(value = "/exhibitions/artworks", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkCreateResponse> createArtwork(
            @Valid @ModelAttribute ArtworkCreateRequest request) {
        ArtworkCreateResponse data = artworkService.createArtwork(request);
        return SuccessResponse.created(data);
    }

    // 3. 작품 기본 정보 수정 (PATCH)
    @PatchMapping(value = "/exhibitions/artworks/{artworkId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkCreateResponse> updateArtwork(
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkUpdateRequest request) {
        ArtworkCreateResponse data = artworkService.updateArtwork(artworkId, request);
        return SuccessResponse.ok(data, "정보가 성공적으로 수정되었습니다.");
    }

    // 4. 작품 삭제 (DELETE)
    @DeleteMapping("/exhibitions/artworks/{artworkId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteArtwork(
            @PathVariable UUID artworkId) {
        artworkService.deleteArtwork(artworkId);
        return SuccessResponse.ok(null, "작품이 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 상세 이미지 관련 API ] ---------------- */

    @PostMapping(value = "/artworks/{artworkId}/images", consumes = "multipart/form-data")
    public SuccessResponse<ArtworkImgCreateResponse> createArtworkImages(
            @PathVariable UUID artworkId,
            @ModelAttribute ArtworkImgListRequest request // List 대신 래퍼 클래스 사용
    ) {
        ArtworkImgCreateResponse response = artworkService.createArtworkImages(artworkId, request.getImages());
        return SuccessResponse.ok(response, "작품 상세 이미지 등록에 성공하였습니다.");
    }

    // 6. 작품 상세 이미지 개별 수정 (PATCH)
    @PatchMapping(value = "/artworks/{artworkId}/images/{imageId}", consumes = "multipart/form-data") // 👈 consumes 추가
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkImgUpdateResponse> updateArtworkImage(
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId,
            @Valid @ModelAttribute ArtworkImgUpdateRequest request) {
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
    @PatchMapping("/artworks/{artworkId}/artists/{artistProfileId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkArtistMappingResponse> updateArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistProfileId,
            @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.updateArtistMapping(artworkId, artistProfileId, request);
        return SuccessResponse.ok(data, "작가 역할이 성공적으로 수정되었습니다.");
    }

    // 10. 작품 작가 매핑 삭제 (DELETE)
    @DeleteMapping("/artworks/{artworkId}/artists/{artistProfileId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistProfileId) {
        artworkService.deleteArtistMapping(artworkId, artistProfileId);
        return SuccessResponse.ok(null, "작가 연결이 성공적으로 해제되었습니다.");
    }

    @GetMapping("/exhibitions/{exhibitionId}/artworks")
    public SuccessResponse<ExhibitionArtworkListResponse> getExhibitionArtworks(
            @PathVariable UUID exhibitionId,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        ExhibitionArtworkListResponse data = artworkService.getExhibitionArtworkList(exhibitionId, zone, category, search);
        return SuccessResponse.ok(data, "작품 목록 및 관람 안내 조회에 성공하였습니다.");
    }

    // 11. 작품 전체 정보 수정 (PUT)
    @PutMapping(value = "/exhibitions/{exhibitionId}/artworks/{artworkId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkUpdateFullResponse> updateArtworkFull(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkUpdateFullRequest request
    ) {
        // 서비스 호출 시 exhibitionId를 같이 넘겨서 zone 검증에 활용합니다.
        ArtworkUpdateFullResponse data = artworkService.updateArtworkFull(exhibitionId, artworkId, request);
        return SuccessResponse.ok(data, "작품 정보 및 연관 데이터가 성공적으로 동기화되었습니다.");
    }

    // 상세 조회 API만 신규 서비스를 타게 합니다.
    @GetMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    public SuccessResponse<ArtworkDetailResponse> getArtworkDetail(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId) {
        return SuccessResponse.ok(artworkDetailService.getArtworkDetail(exhibitionId, artworkId), "작품 상세 조회 성공");
    }
}