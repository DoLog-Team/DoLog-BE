package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.web.dto.request.basic.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.dolog.server.domain.exhibition.service.ExhibitionService;
import com.dolog.server.domain.exhibition.web.dto.response.basic.*;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(null);
            }
        });
    }

    // 전시회 전체 목록 조회
    @Operation(summary = "전시회 전체 목록 조회")
    @GetMapping
    public ResponseEntity<SuccessResponse<List<ExhibitionListItemResponse>>> getExhibitions(
            @RequestParam(name = "is_public", required = false) Boolean isPublic,
            @RequestParam(name = "univ_name", required = false) String univName,
            @RequestParam(name = "exhibition_type", required = false) ExhibitionType exhibitionType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(SuccessResponse.ok(exhibitionService.getExhibitions(isPublic, univName, exhibitionType, search)));
    }

    // slug로 uuid 조회
    @Operation(summary = "slug로 전시회 uuid 조회")
    @GetMapping("/resolve/{slug}")
    public SuccessResponse<ExhibitionResolveResponse> resolveSlug(
            @PathVariable String slug) {
        return SuccessResponse.ok(exhibitionService.resolveSlug(slug));
    }

    // 메인 전시회 조회
    @Operation(summary = "메인 전시회 조회")
    @GetMapping("/main")
    public SuccessResponse<ExhibitionMainResponse> getMainExhibitions(
            @RequestParam(required = false) String sort
    ) {
        return SuccessResponse.ok(
                exhibitionService.getMainExhibitions(sort),
                "메인 전시 조회 성공"
        );
    }

    // 전시회 기본+상세+장소 통합 조회
    @Operation(summary = "전시회 정보 통합 조회")
    @GetMapping("/{exhibitionId}/details")
    public SuccessResponse<ExhibitionIntegratedResponse> getExhibitionDetails(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionService.getExhibitionDetails(exhibitionId));
    }

    // 전시회 푸터 정보 조회
    @Operation(summary = "전시회 푸터 정보 통합 조회")
    @GetMapping("/{exhibitionId}/footer-info")
    public SuccessResponse<ExhibitionFooterResponse> getFooterInfo(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionService.getFooterInfo(exhibitionId), "전시회 푸터 정보 조회가 완료되었습니다.");
    }

    // 전시회 메타데이터 조회 (OG tag)
    @Operation(summary = "전시회 메타 데이터 조회")
    @GetMapping("/{exhibitionId}/meta")
    public SuccessResponse<ExhibitionMetaResponse> getExhibitionMeta(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionService.getExhibitionMeta(exhibitionId));
    }

    @Operation(summary = "전시회 OG 메타 정보 수정")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping(value = "/{exhibitionId}/meta", consumes = "multipart/form-data")
    public SuccessResponse<ExhibitionMetaResponse> updateExhibitionMeta(
            @PathVariable UUID exhibitionId,
            @ModelAttribute ExhibitionMetaUpdateRequest request
    ) throws IOException {
        return SuccessResponse.ok(exhibitionService.updateExhibitionMeta(exhibitionId, request));
    }


    // 전시회 기본정보 등록
    @Operation(summary = "전시회 기본정보 등록")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping
    public ResponseEntity<SuccessResponse<ExhibitionCreateResponse>> createExhibition(
            @Valid @RequestBody ExhibitionCreateRequest request) {
        ExhibitionCreateResponse response = exhibitionService.createExhibition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    // 전시 로그인 코드 재발급
    @Operation(summary = "전시 로그인 코드 발급·재발급")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping("/{exhibitionId}/entry-code")
    public ResponseEntity<SuccessResponse<EntryCodeResponse>> reissueEntryCode(
            @PathVariable UUID exhibitionId, @Valid @RequestBody EntryCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(exhibitionService.reissueEntryCode(exhibitionId, request.getExpiresAt())));
    }

    // 전시회 기본정보 수정
    @Operation(summary = "전시회 기본정보 수정")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/{exhibitionId}")
    public ResponseEntity<SuccessResponse<ExhibitionMessageResponse>> updateExhibition(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionUpdateRequest request) {
        ExhibitionMessageResponse response = exhibitionService.updateExhibition(exhibitionId, request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    // 전시회 삭제
    @Operation(summary = "전시회 삭제")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @DeleteMapping("/{exhibitionId}")
    public ResponseEntity<SuccessResponse<ExhibitionMessageResponse>> deleteExhibition(
            @PathVariable UUID exhibitionId) {
        ExhibitionMessageResponse response = exhibitionService.deleteExhibition(exhibitionId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

//   -----------------------------------------------------------------------------

    // 전시 상세정보 등록/수정
    @Operation(summary = "전시회 상세정보 등록/수정")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PutMapping(value = "/{exhibitionId}/details", consumes = "multipart/form-data")
    public SuccessResponse<ExhibitionDetailUpsertResponse> upsertExhibitionDetail(
            @PathVariable UUID exhibitionId,
            @Valid @ModelAttribute ExhibitionDetailUpsertRequest request
    ) throws java.io.IOException {
        ExhibitionDetailUpsertResponse data = exhibitionService.upsertExhibitionDetail(exhibitionId, request);
        return SuccessResponse.ok(data);
    }

    @Operation(summary = "작가 참여 코드 발급·재발급")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping("/{exhibitionId}/artist-join-code")
    public ResponseEntity<SuccessResponse<ArtistJoinCodeResponse>>
    reissueArtistJoinCode(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ArtistJoinCodeRequest request
    ) {
        ArtistJoinCodeResponse response =
                exhibitionService.reissueArtistJoinCode(
                        exhibitionId,
                        request.getExpiresAt()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(
                        response,
                        "작가 참여 코드 재발급 성공"
                ));
    }

    // 전시 게시하기
    @Operation(summary = "전시 게시하기")
    @PreAuthorize("hasRole('EXHIBITION_ADMIN')")
    @PostMapping("/{exhibitionId}/publish")
    public SuccessResponse<ExhibitionPublishResponse> publishExhibition(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId
    ) {
        ExhibitionPublishResponse data = exhibitionService.publishExhibition(exhibitionId, user.getId());
        return SuccessResponse.ok(data, "전시 게시 성공");
    }

    // 게시 기간 연장
    @Operation(summary = "게시 기간 연장")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PatchMapping("/{exhibitionId}/expires-at")
    public SuccessResponse<ExhibitionPublishResponse> extendExpiresAt(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExpiresAtUpdateRequest request
    ) {
        ExhibitionPublishResponse data = exhibitionService.extendExpiresAt(exhibitionId, request.getExpiresAt());
        return SuccessResponse.ok(data, "게시 기간 연장 성공");
    }

    // 전시 어드민 홈 통합 조회 (전시 정보 보기 화면 최초 호출용)
    @Operation(summary = "전시 어드민 홈 통합 조회")
    @PreAuthorize("hasRole('EXHIBITION_ADMIN')")
    @GetMapping("/{exhibitionId}/admin-home")
    public SuccessResponse<ExhibitionAdminHomeResponse> getAdminHome(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId
    ) {
        ExhibitionAdminHomeResponse data = exhibitionService.getAdminHome(exhibitionId, user.getId());
        return SuccessResponse.ok(data, "전시 어드민 홈 조회 성공");
    }
}
