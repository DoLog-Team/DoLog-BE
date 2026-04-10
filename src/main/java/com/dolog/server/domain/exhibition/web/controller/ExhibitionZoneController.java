package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionZoneService;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionZoneController {

    private final ExhibitionZoneService exhibitionZoneService;

    // 전시 구역 목록 조회
    @GetMapping("/{exhibitionId}/zones")
    public SuccessResponse<ExhibitionZoneListResponse> getZones(
            @PathVariable UUID exhibitionId
    ) {
        ExhibitionZoneListResponse data = exhibitionZoneService.getZones(exhibitionId);
        return SuccessResponse.ok(data, "전시 구역 조회가 완료되었습니다.");
    }

    // 전시 구역 생성
    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/{exhibitionId}/zones")
    public ResponseEntity<SuccessResponse<ExhibitionZoneCreateResponse>> createZone(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionZoneCreateRequest request
    ) {
        ExhibitionZoneCreateResponse data = exhibitionZoneService.createZone(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data));
    }

    // 전시 구역 수정
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/{exhibitionId}/zones/{zoneId}")
    public SuccessResponse<ExhibitionZoneUpdateResponse> updateZone(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID zoneId,
            @RequestBody ExhibitionZoneUpdateRequest request
    ) {
        ExhibitionZoneUpdateResponse data = exhibitionZoneService.updateZone(exhibitionId, zoneId, request);
        return SuccessResponse.ok(data, "구역 정보가 성공적으로 수정되었습니다.");
    }

    // 전시 구역 삭제
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/{exhibitionId}/zones/{zoneId}")
    public SuccessResponse<Void> deleteZone(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID zoneId
    ) {
        exhibitionZoneService.deleteZone(exhibitionId, zoneId);
        return SuccessResponse.ok(null, "전시 구역이 삭제되었습니다.");
    }
}
