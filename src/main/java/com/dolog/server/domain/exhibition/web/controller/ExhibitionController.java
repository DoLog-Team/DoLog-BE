package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionService;
import com.dolog.server.domain.exhibition.web.dto.request.AddArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionDetailUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.RemoveArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.response.*;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionController {

    private final ExhibitionService exhibitionService;

    // 전시회 전체 목록 조회
    @GetMapping
    public ResponseEntity<SuccessResponse<List<ExhibitionListItemResponse>>> getExhibitions(
            @RequestParam(name = "is_public", required = false) Boolean isPublic,
            @RequestParam(name = "univ_name", required = false) String univName,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(SuccessResponse.ok(exhibitionService.getExhibitions(isPublic, univName, search)));
    }

    // 메인 전시회 조회
    @GetMapping("/main")
    public ResponseEntity<SuccessResponse<ExhibitionMainResponse>> getMainExhibitions() {
        return ResponseEntity.ok(SuccessResponse.ok(exhibitionService.getMainExhibitions()));
    }

    // 전시회 기본정보 등록
    @PostMapping
    public ResponseEntity<SuccessResponse<ExhibitionCreateResponse>> createExhibition(
            @Valid @RequestBody ExhibitionCreateRequest request) {
        ExhibitionCreateResponse response = exhibitionService.createExhibition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    // 전시회 기본정보 수정
    @PatchMapping("/{exhibitionId}")
    public ResponseEntity<SuccessResponse<ExhibitionMessageResponse>> updateExhibition(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionUpdateRequest request) {
        ExhibitionMessageResponse response = exhibitionService.updateExhibition(exhibitionId, request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    // 전시회 삭제
    @DeleteMapping("/{exhibitionId}")
    public ResponseEntity<SuccessResponse<ExhibitionMessageResponse>> deleteExhibition(
            @PathVariable UUID exhibitionId) {
        ExhibitionMessageResponse response = exhibitionService.deleteExhibition(exhibitionId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

//   -----------------------------------------------------------------------------

    // 전시 상세정보 등록/수정
    @PutMapping("/{exhibitionId}/details")
    public SuccessResponse<ExhibitionDetailUpsertResponse> upsertExhibitionDetail(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionDetailUpsertRequest request
    ) {
        ExhibitionDetailUpsertResponse data = exhibitionService.upsertExhibitionDetail(exhibitionId, request);
        return SuccessResponse.ok(data);
    }
}
