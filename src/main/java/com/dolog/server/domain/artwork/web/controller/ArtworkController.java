package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.order.ArtworkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artwork.service.artwork.query.ArtworkDetailQueryService;
import com.dolog.server.domain.artwork.service.ArtworkService;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artwork")
@RestController
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;
    private final ArtworkDetailQueryService artworkDetailService;
    private final ArtworkOrderService artworkOrderService;

    // 1. 작품 전체 목록 조회
    @Operation(summary = "작품 전체 목록 조회")
    @GetMapping("/artworks")
    public SuccessResponse<Object> getArtworks(
            @RequestParam(required = false) Boolean main,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort
    ) {
        Object data = artworkService.getArtworks(main, category, search, sort);
        return SuccessResponse.ok(data);
    }

    // 2. 작품 기본 정보 등록
    @Operation(summary = "작품 등록")
    @PostMapping(value = "/exhibitions/artworks", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkCreateResponse> createArtwork(
            @Valid @ModelAttribute ArtworkCreateRequest request) {
        ArtworkCreateResponse data = artworkService.createArtwork(request);
        return SuccessResponse.created(data);
    }

    // 3. 작품 기본 정보 수정 (PATCH)
    @Operation(summary = "작품 기본 정보 수정")
    @PatchMapping(value = "/exhibitions/artworks/{artworkId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkCreateResponse> updateArtwork(
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkUpdateRequest request) {
        ArtworkCreateResponse data = artworkService.updateArtwork(artworkId, request);
        return SuccessResponse.ok(data, "정보가 성공적으로 수정되었습니다.");
    }

    // 4. 작품 삭제 (DELETE)
    @Operation(summary = "작품 기본 정보 삭제")
    @DeleteMapping("/exhibitions/artworks/{artworkId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteArtwork(
            @PathVariable UUID artworkId) {
        artworkService.deleteArtwork(artworkId);
        return SuccessResponse.ok(null, "작품이 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 상세 이미지 관련 API ] ---------------- */

    @Operation(summary = "작품 상세 이미지 등록")
    @PostMapping(value = "/artworks/{artworkId}/images", consumes = "multipart/form-data")
    public SuccessResponse<ArtworkImgCreateResponse> createArtworkImages(
            @PathVariable UUID artworkId,
            @ModelAttribute ArtworkImgListRequest request // List 대신 래퍼 클래스 사용
    ) {
        ArtworkImgCreateResponse response = artworkService.createArtworkImages(artworkId, request.getImages());
        return SuccessResponse.ok(response, "작품 상세 이미지 등록에 성공하였습니다.");
    }

    // 6. 작품 상세 이미지 개별 수정 (PATCH)
    @Operation(summary = "작품 상세 이미지 수정")
    @PatchMapping(value = "/artworks/{artworkId}/images/{imageId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkImgUpdateResponse> updateArtworkImage(
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId,
            @Valid @ModelAttribute ArtworkImgUpdateRequest request) {
        ArtworkImgUpdateResponse data = artworkService.updateArtworkImage(artworkId, imageId, request);
        return SuccessResponse.ok(data, "상세 이미지 정보가 성공적으로 수정되었습니다.");
    }

    // 7. 작품 상세 이미지 개별 삭제 (DELETE)
    @Operation(summary = "작품 상세 이미지 삭제")
    @DeleteMapping("/artworks/{artworkId}/images/{imageId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteArtworkImage(
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId) {
        artworkService.deleteArtworkImage(artworkId, imageId);
        return SuccessResponse.ok(null, "상세 이미지가 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 작가 매핑 관련 API ] ---------------- */

    // 8. 작품 작가 매핑 등록 (POST)
    @Operation(summary = "작품 공동 작가 등록")
    @PostMapping("/artworks/{artworkId}/artists")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkArtistMappingResponse> createArtistMapping(
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.createArtistMapping(artworkId, request);
        return SuccessResponse.created(data);
    }

    // 9. 작품 작가 매핑 수정 (PATCH)
    @Operation(summary = "작품 공동 작가 수정")
    @PatchMapping("/artworks/{artworkId}/artists/{artistProfileId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkArtistMappingResponse> updateArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistProfileId,
            @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.updateArtistMapping(artworkId, artistProfileId, request);
        return SuccessResponse.ok(data, "작가 역할이 성공적으로 수정되었습니다.");
    }

    // 10. 작품 작가 매핑 삭제 (DELETE)
    @Operation(summary = "작품 공동 작가 삭제")
    @DeleteMapping("/artworks/{artworkId}/artists/{artistProfileId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteArtistMapping(
            @PathVariable UUID artworkId,
            @PathVariable UUID artistProfileId) {
        artworkService.deleteArtistMapping(artworkId, artistProfileId);
        return SuccessResponse.ok(null, "작가 연결이 성공적으로 해제되었습니다.");
    }

    @Operation(summary = "작품 목록 조회")
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
    @Operation(summary = "작품 전체 정보 수정")
    @PutMapping(value = "/exhibitions/{exhibitionId}/artworks/{artworkId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtworkUpdateFullResponse> updateArtworkFull(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkUpdateFullRequest request
    ) {
        // 서비스 호출 시 exhibitionId를 같이 넘겨서 zone 검증에 활용합니다.
        ArtworkUpdateFullResponse data = artworkService.updateArtworkFull(exhibitionId, artworkId, request);
        return SuccessResponse.ok(data, "작품 정보 및 연관 데이터가 성공적으로 동기화되었습니다.");
    }

    @Operation(summary = "작품 상세 조회")
    @GetMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    public SuccessResponse<ArtworkDetailResponse> getArtworkDetail(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId) {
        return SuccessResponse.ok(artworkDetailService.getArtworkDetail(exhibitionId, artworkId), "작품 상세 조회 성공");
    }

    /* ---------------- [ 작품 순서 정렬 관련 API ] ---------------- */

    // 12. 전시회 내 모든 작품 순서 일괄 재정렬 및 DB 저장 (PUT)
    @Operation(summary = "전시회 내 전체 작품 순서 재정렬", description = "전시회 내의 작품들을 각 Zone별로 [1순위: 작가 가나다, 2순위: 작품명 가나다] 순서로 정렬하여 orderIndex(10, 20, 30...)를 DB에 일괄 갱신합니다.")
    @PutMapping("/exhibitions/{exhibitionId}/artworks/reorder")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> reorderExhibitionArtworks(
            @PathVariable UUID exhibitionId) {

        artworkOrderService.reorderArtworkIndices(exhibitionId);
        return SuccessResponse.ok(null, "전시회 내 모든 작품의 순서가 성공적으로 재정렬되어 저장되었습니다.");
    }
}