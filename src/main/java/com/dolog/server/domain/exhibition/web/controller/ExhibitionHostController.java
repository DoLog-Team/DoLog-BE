package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionHostService;
import com.dolog.server.domain.exhibition.web.dto.request.host.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.host.HostSnsRequest;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.HostSnsResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionHostController {

    private final ExhibitionHostService exhibitionHostService;

    @PreAuthorize("hasRole('DEVELOPER')")
    @PutMapping("/{exhibitionId}/host")
    public SuccessResponse<ExhibitionHostResponse> upsertHost(
            @PathVariable UUID exhibitionId,
            @RequestBody ExhibitionHostUpsertRequest request) {

        ExhibitionHostResponse response = exhibitionHostService.upsertExhibitionHost(exhibitionId, request);
        return SuccessResponse.ok(response, "주최 기관 정보 등록/수정 성공");
    }

//===============[SNS]================
// SNS 추가
@PreAuthorize("hasRole('DEVELOPER')")
@PostMapping("/{exhibitionId}/host/sns")
public SuccessResponse<HostSnsResponse> addHostSns(
        @PathVariable UUID exhibitionId, // exhibitionId로 받기
        @RequestBody HostSnsRequest request) {
    return SuccessResponse.ok(exhibitionHostService.addHostSns(exhibitionId, request), "주최 기관 SNS 등록 성공");
}

    // SNS 목록 조회
    @GetMapping("/{exhibitionId}/host/sns")
    public SuccessResponse<List<HostSnsResponse>> getHostSnsList(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionHostService.getHostSnsList(exhibitionId), "주최 기관 SNS 목록 조회 성공");
    }

    // SNS 수정
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/host/sns/{snsId}")
    public SuccessResponse<List<HostSnsResponse>> updateHostSns(
            @PathVariable UUID snsId,
            @RequestBody HostSnsRequest request) {
        return SuccessResponse.ok(exhibitionHostService.updateHostSns(snsId, request), "주최 기관 SNS 수정 성공");
    }

    // SNS 삭제
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/host/sns/{snsId}")
    public SuccessResponse<List<HostSnsResponse>> deleteHostSns(@PathVariable UUID snsId) {
        return SuccessResponse.ok(exhibitionHostService.deleteHostSns(snsId), "주최 기관 SNS 삭제 성공");
    }

}
