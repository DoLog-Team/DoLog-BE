package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionServiceImpl;
import com.dolog.server.domain.exhibition.web.dto.request.AddArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
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

    private final ExhibitionServiceImpl exhibitionService;

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
    // 전시 참여 작가 목록 조회
    @GetMapping("/{exhibitionId}/artists")
    public SuccessResponse<List<ExhibitionArtistListResponse>> getArtists(
            @PathVariable UUID exhibitionId
    ) {
        List<ExhibitionArtistListResponse> data =
                exhibitionService.getArtistsByExhibition(exhibitionId);

        return SuccessResponse.ok(
                data,
                "전시 작가 목록 조회 성공"
        );
    }

    //전시 작가 추가
    @PostMapping("/{exhibitionId}/artists")
    public SuccessResponse<ExhibitionArtistAddResponse> addArtist(
            @PathVariable UUID exhibitionId,
            @RequestBody AddArtistRequest request
    ) {
        ExhibitionArtistAddResponse data =
                exhibitionService.addArtistToExhibition(exhibitionId, request.getArtistId());

        return SuccessResponse.ok(
                data,
                "작가가 전시에 추가되었습니다."
        );
    }

    // 전시 장소 정보 등록
    @PostMapping("/{exhibitionId}/map")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<ExhibitionMapCreateResponse>> createExhibitionMap(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionMapCreateRequest request) {
        ExhibitionMapCreateResponse response = exhibitionService.createExhibitionMap(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    // 전시 장소 정보 수정
    @PatchMapping("/{exhibitionId}/map")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<ExhibitionMapUpdateResponse>> updateExhibitionMap(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionMapUpdateRequest request) {
        ExhibitionMapUpdateResponse response = exhibitionService.updateExhibitionMap(exhibitionId, request);
        return ResponseEntity.ok(SuccessResponse.ok(response, "장소 정보 수정에 성공하였습니다."));
    }

    //전시 작가 삭제
    @DeleteMapping("/{exhibitionId}/artists")
    public SuccessResponse<ExhibitionArtistRemoveResponse> removeArtist(
            @PathVariable UUID exhibitionId,
            @RequestBody RemoveArtistRequest request
    ) {
        ExhibitionArtistRemoveResponse data =
                exhibitionService.removeArtistFromExhibition(exhibitionId, request.getArtistId());

        return SuccessResponse.ok(data);
    }


}
