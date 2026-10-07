package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artwork.service.artwork.query.ArtworkDetailQueryService;
import com.dolog.server.domain.artwork.service.ArtworkService;
import com.dolog.server.domain.artwork.service.view.ArtworkViewService;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import com.dolog.server.domain.like.exception.LikeException;
import com.dolog.server.domain.like.support.VisitorIdResolver;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artwork")
@RestController
@RequiredArgsConstructor
public class ArtworkController {

    private final ArtworkService artworkService;
    private final ArtworkDetailQueryService artworkDetailService;
    private final VisitorIdResolver visitorIdResolver;
    private final ArtworkViewService artworkViewService;
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
    @Operation(summary = "작품 등록", description = "로그인한 작가 본인의 작품을 전시 미소속(DRAFT) 상태로 등록합니다.")
    @PostMapping(value = "/artworks", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public ResponseEntity<SuccessResponse<ArtworkCreateResponse>> createArtwork(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute ArtworkCreateRequest request) {
        ArtworkCreateResponse data = artworkService.createArtwork(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data));
    }

    // 3. 작품 기본 정보 수정 (PATCH)
    @Operation(summary = "작품 기본 정보 수정", description = "본인 작품만 수정할 수 있으며 보낸 필드만 반영합니다.")
    @PatchMapping(value = "/artworks/{artworkId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<ArtworkCreateResponse> updateArtwork(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkUpdateRequest request) {
        ArtworkCreateResponse data = artworkService.updateArtwork(user.getId(), artworkId, request);
        return SuccessResponse.ok(data, "정보가 성공적으로 수정되었습니다.");
    }

    // 4. 작품 삭제 (DELETE)
    @Operation(summary = "작품 기본 정보 삭제", description = "본인 작품만 삭제할 수 있습니다.")
    @DeleteMapping("/artworks/{artworkId}")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<Void> deleteArtwork(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId) {
        artworkService.deleteArtwork(user.getId(), artworkId);
        return SuccessResponse.ok(null, "작품이 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 출품 / 공개 상태 API ] ---------------- */

    @Operation(summary = "작품 전시회에 출품하기", description = "참여 중(JOINED)인 전시의 구역에 출품합니다. 순서는 구역 마지막으로 정해집니다.")
    @PutMapping(value = "/artworks/{artworkId}/exhibition", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<ArtworkSubmitResponse> submitArtwork(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkSubmitRequest request) {
        ArtworkSubmitResponse data = artworkService.submitArtwork(user.getId(), artworkId, request);
        return SuccessResponse.ok(data, "작품이 전시에 출품되었습니다.");
    }

    @Operation(summary = "작품 출품 취소", description = "전시 연결을 해제하고 전시 미소속 상태로 되돌립니다.")
    @DeleteMapping("/artworks/{artworkId}/exhibition")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<Void> cancelSubmission(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId) {
        artworkService.cancelSubmission(user.getId(), artworkId);
        return SuccessResponse.ok(null, "작품 출품이 취소되었습니다.");
    }

    @Operation(summary = "작품 공개/비공개 처리 (작가 어드민)", description = "DRAFT/PUBLISHED 전환. 출품된 작품은 전시의 필수 항목을 채워야 공개할 수 있습니다.")
    @PatchMapping("/artworks/{artworkId}/exhibition")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<ArtworkStatusResponse> changeArtworkStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkStatusUpdateRequest request) {
        ArtworkStatusResponse data = artworkService.changeArtworkStatus(user.getId(), artworkId, request);
        return SuccessResponse.ok(data, request.status() == ArtworkStatus.PUBLISHED
                ? "작품이 공개 처리되었습니다." : "작품이 비공개 처리되었습니다.");
    }

    @Operation(summary = "작품 숨김/재공개 처리 (전시 어드민)", description = "전시 URL 에서만 숨깁니다. 두록 URL 노출과 공개 상태는 그대로입니다.")
    @PatchMapping("/artworks/{artworkId}/hidden")
    @PreAuthorize("hasRole('EXHIBITION_ADMIN')")
    public SuccessResponse<ArtworkHiddenResponse> changeArtworkHidden(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkHiddenUpdateRequest request) {
        ArtworkHiddenResponse data = artworkService.changeArtworkHidden(user.getId(), artworkId, request);
        return SuccessResponse.ok(data, request.hidden() ? "작품이 숨김 처리되었습니다." : "작품이 재공개되었습니다.");
    }

    /* ---------------- [ 상세 이미지 관련 API ] ---------------- */

    @Operation(summary = "작품 상세 이미지 등록", description = "두록 어드민 또는 작품의 작가 본인. 순서를 안 보내면 기존 이미지 뒤에 붙습니다.")
    @PostMapping(value = "/artworks/{artworkId}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'ARTIST_ADMIN')")
    public ResponseEntity<SuccessResponse<ArtworkImgCreateResponse>> createArtworkImages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @ModelAttribute ArtworkImgListRequest request // List 대신 래퍼 클래스 사용
    ) {
        ArtworkImgCreateResponse response = artworkService.createArtworkImages(
                user.getId(), isDologAdmin(user), artworkId, request.getImages());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(response, "작품 상세 이미지 등록에 성공하였습니다."));
    }

    // 6. 작품 상세 이미지 개별 수정 (PATCH)
    @Operation(summary = "작품 상세 이미지 수정")
    @PatchMapping(value = "/artworks/{artworkId}/images/{imageId}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'ARTIST_ADMIN')")
    public SuccessResponse<ArtworkImgUpdateResponse> updateArtworkImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId,
            @Valid @ModelAttribute ArtworkImgUpdateRequest request) {
        ArtworkImgUpdateResponse data = artworkService.updateArtworkImage(
                user.getId(), isDologAdmin(user), artworkId, imageId, request);
        return SuccessResponse.ok(data, "상세 이미지 정보가 성공적으로 수정되었습니다.");
    }

    // 7. 작품 상세 이미지 개별 삭제 (DELETE)
    @Operation(summary = "작품 상세 이미지 삭제")
    @DeleteMapping("/artworks/{artworkId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'ARTIST_ADMIN')")
    public SuccessResponse<Void> deleteArtworkImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @PathVariable UUID imageId) {
        artworkService.deleteArtworkImage(user.getId(), isDologAdmin(user), artworkId, imageId);
        return SuccessResponse.ok(null, "상세 이미지가 성공적으로 삭제되었습니다.");
    }

    /* ---------------- [ 작가 매핑 관련 API ] ---------------- */

    // 8. 작품 공동 작가 등록 (POST)
    @Operation(summary = "작품 공동 작가 등록", description = "본인 작품이고 전시에 출품된 경우에만, 같은 전시에 참여 중인 작가의 프로필을 연결합니다.")
    @PostMapping("/artworks/{artworkId}/artists")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public ResponseEntity<SuccessResponse<ArtworkArtistMappingResponse>> createArtistMapping(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @Valid @RequestBody ArtworkArtistMappingRequest request) {
        ArtworkArtistMappingResponse data = artworkService.createArtistMapping(user.getId(), artworkId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data));
    }

    // 9. 작품 공동 작가 역할 수정 (PATCH)
    @Operation(summary = "작품 공동 작가 수정", description = "본인 작품에 연결된 작가(artistId)의 역할을 수정합니다.")
    @PatchMapping("/artworks/{artworkId}/artists/{artistId}")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<ArtworkArtistMappingResponse> updateArtistMapping(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @PathVariable UUID artistId,
            @Valid @RequestBody ArtworkArtistRoleRequest request) {
        ArtworkArtistMappingResponse data = artworkService.updateArtistMapping(user.getId(), artworkId, artistId, request);
        return SuccessResponse.ok(data, "작가 역할이 성공적으로 수정되었습니다.");
    }

    // 10. 작품 공동 작가 삭제 (DELETE)
    @Operation(summary = "작품 공동 작가 삭제", description = "본인 작품에 연결된 작가(artistId)를 해제합니다. 마지막 남은 작가는 해제할 수 없습니다.")
    @DeleteMapping("/artworks/{artworkId}/artists/{artistId}")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<Void> deleteArtistMapping(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID artworkId,
            @PathVariable UUID artistId) {
        artworkService.deleteArtistMapping(user.getId(), artworkId, artistId);
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
    @Operation(summary = "작품 전체 정보 수정", description = "두록 어드민 전용. path 의 전시에 출품된 작품만 다룹니다. "
            + "공동 작가는 그 전시의 프로필(artistProfileIds)로 맞추고, images 를 보내면 그 목록으로 이미지를 맞춥니다. "
            + "선택 값은 안 보내면 기존 값을 유지합니다.")
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

    @Operation(summary = "작품 상세 조회 (전시 URL)", description = "공개이면서 작품과 작품 그룹이 숨김이 아닌 작품만 조회합니다. 전시의 항목 숨김 설정이 적용됩니다.")
    @GetMapping("/exhibitions/{exhibitionId}/artworks/{artworkId}")
    public SuccessResponse<ArtworkDetailResponse> getArtworkDetail(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID artworkId,
            HttpServletRequest request) {
        return SuccessResponse.ok(
                artworkDetailService.getArtworkDetail(exhibitionId, artworkId, visitorIdOf(request)),
                "작품 상세 조회 성공");
    }

    @Operation(summary = "작품 상세 조회 (두록 URL)", description = "공개 작품이면 숨김 여부와 상관없이 조회합니다.")
    @GetMapping("/artworks/{artworkId}")
    public SuccessResponse<ArtworkDetailResponse> getDologArtworkDetail(
            @PathVariable UUID artworkId,
            HttpServletRequest request) {
        return SuccessResponse.ok(
                artworkDetailService.getDologArtworkDetail(artworkId, visitorIdOf(request)),
                "작품 상세 조회 성공");
    }

    @Operation(summary = "작품 조회수 기록",
            description = "비로그인 가능. 이용자가 작품 상세 페이지에 일정 시간 머문 뒤 브라우저에서 한 번 호출합니다. "
                    + "같은 방문자(visitor_id)는 작품마다 한 번만 셉니다. "
                    + "visitor_id 가 없으면 좋아요와 같은 방식으로 새로 발급해 쿠키로 내려줍니다.")
    @PostMapping("/artworks/{artworkId}/views")
    public SuccessResponse<ArtworkViewResponse> recordArtworkView(
            @PathVariable UUID artworkId,
            HttpServletRequest request,
            HttpServletResponse response) {
        String visitorId = visitorIdResolver.resolveOrIssue(request, response, null);
        return SuccessResponse.ok(artworkViewService.recordView(artworkId, visitorId), "조회수가 기록되었습니다.");
    }

    // 좋아요 여부 확인용이라, 방문자 ID 형식이 잘못돼도 상세 조회는 막지 않는다.
    private String visitorIdOf(HttpServletRequest request) {
        try {
            return visitorIdResolver.resolve(request, null).orElse(null);
        } catch (LikeException e) {
            return null;
        }
    }

    /* ---------------- [ 작품 순서 정렬 관련 API ] ---------------- */

    // 12. 전시회 내 모든 작품 순서 일괄 재정렬 및 DB 저장 (PUT)
    @Operation(summary = "전시회 내 전체 작품 순서 재정렬", description = "두록 어드민 또는 본인 전시의 전시 어드민. 전시회 내의 작품들을 각 Zone별로 [1순위: 작가 가나다, 2순위: 작품명 가나다] 순서로 정렬하여 orderIndex(10, 20, 30...)를 DB에 일괄 갱신합니다.")
    @PutMapping("/exhibitions/{exhibitionId}/artworks/reorder")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')")
    public SuccessResponse<Void> reorderExhibitionArtworks(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId) {

        artworkOrderService.reorderArtworkIndices(exhibitionId, user.getId(), isDologAdmin(user));
        return SuccessResponse.ok(null, "전시회 내 모든 작품의 순서가 성공적으로 재정렬되어 저장되었습니다.");
    }

    private boolean isDologAdmin(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DOLOG_ADMIN".equals(authority.getAuthority()));
    }
}