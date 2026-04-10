package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionZoneService;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.response.code.BaseResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionZoneController {

    private final ExhibitionZoneService exhibitionZoneService;

    // 전시 구역 생성
    @PostMapping("/{exhibitionId}/zones")
    public ResponseEntity<SuccessResponse<ExhibitionZoneCreateResponse>> createZone(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionZoneCreateRequest request
    ) {
        ExhibitionZoneCreateResponse data = exhibitionZoneService.createZone(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                SuccessResponse.of(data, new BaseResponseCode() {
                    public String getCode() { return "SUCCESS_201"; }
                    public int getHttpStatus() { return 201; }
                    public String getMessage() { return "새로운 전시 구역이 생성되었습니다."; }
                })
        );
    }

    // 전시 구역 수정
    @PatchMapping("/{exhibitionId}/zones/{zoneId}")
    public SuccessResponse<ExhibitionZoneUpdateResponse> updateZone(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID zoneId,
            @RequestBody ExhibitionZoneUpdateRequest request
    ) {
        ExhibitionZoneUpdateResponse data = exhibitionZoneService.updateZone(exhibitionId, zoneId, request);
        return SuccessResponse.ok(data, "구역 정보가 성공적으로 수정되었습니다.");
    }
}
