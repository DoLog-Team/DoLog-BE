package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionMapService;
import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "exhibition-map")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionMapController {

    private final ExhibitionMapService exhibitionMapService;

    // 전시 장소 정보 등록
    @Operation(summary = "전시회 장소 정보 생성")
    @PostMapping("/{exhibitionId}/map")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<ExhibitionMapCreateResponse>> createExhibitionMap(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionMapCreateRequest request) {
        ExhibitionMapCreateResponse response = exhibitionMapService.createExhibitionMap(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    // 전시 장소 정보 수정
    @Operation(summary = "전시회 장소 정보 수정")
    @PatchMapping("/{exhibitionId}/map")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<ExhibitionMapUpdateResponse>> updateExhibitionMap(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionMapUpdateRequest request) {
        ExhibitionMapUpdateResponse response = exhibitionMapService.updateExhibitionMap(exhibitionId, request);
        return ResponseEntity.ok(SuccessResponse.ok(response, "장소 정보 수정에 성공하였습니다."));
    }

    // 전시 장소 정보 삭제
    @Operation(summary = "전시회 장소 정보 삭제")
    @DeleteMapping("/{exhibitionId}/map")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<SuccessResponse<Void>> deleteExhibitionMap(
            @PathVariable UUID exhibitionId) {
        exhibitionMapService.deleteExhibitionMap(exhibitionId);
        return ResponseEntity.ok(SuccessResponse.ok(null, "장소 정보 삭제에 성공하였습니다."));
    }
}
