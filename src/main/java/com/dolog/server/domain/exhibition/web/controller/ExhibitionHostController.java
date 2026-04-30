package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionHostService;
import com.dolog.server.domain.exhibition.web.dto.request.host.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.host.HostSnsRequest;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostDetailResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.HostSnsResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition-host")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionHostController {

    private final ExhibitionHostService exhibitionHostService;

    @Operation(summary = "주최기관 등록/교체")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PutMapping(value = "/{exhibitionId}/host", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ExhibitionHostResponse> upsertHost(
            @PathVariable UUID exhibitionId,
            @ModelAttribute ExhibitionHostUpsertRequest request) { // @RequestBody 대신 @ModelAttribute

        ExhibitionHostResponse response = exhibitionHostService.upsertExhibitionHost(exhibitionId, request);
        return SuccessResponse.ok(response, "주최 기관 정보 등록/수정 성공");
    }

    @Operation(summary = "주최기관 조회")
    @GetMapping("/{exhibitionId}/host")
    public SuccessResponse<ExhibitionHostDetailResponse> getHost(
            @PathVariable UUID exhibitionId
    ) {
        return SuccessResponse.ok(
                exhibitionHostService.getExhibitionHost(exhibitionId),
                "전시 주최기관 조회 성공"
        );
    }

//===============[SNS]================
// SNS 추가
@Operation(summary = "주최기관 SNS 추가")
@PreAuthorize("hasRole('DEVELOPER')")
@PostMapping("/{exhibitionId}/host/sns")
public SuccessResponse<HostSnsResponse> addHostSns(
        @PathVariable UUID exhibitionId, // exhibitionId로 받기
        @RequestBody HostSnsRequest request) {
    return SuccessResponse.ok(exhibitionHostService.addHostSns(exhibitionId, request), "주최 기관 SNS 등록 성공");
}

    // SNS 목록 조회
    @Operation(summary = "주최기관 SNS 조회")
    @GetMapping("/{exhibitionId}/host/sns")
    public SuccessResponse<List<HostSnsResponse>> getHostSnsList(
            @PathVariable UUID exhibitionId) {
        return SuccessResponse.ok(exhibitionHostService.getHostSnsList(exhibitionId), "주최 기관 SNS 목록 조회 성공");
    }

    // SNS 수정
    @Operation(summary = "주최기관 SNS 링크 수정")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/host/sns/{snsId}")
    public SuccessResponse<List<HostSnsResponse>> updateHostSns(
            @PathVariable UUID snsId,
            @RequestBody HostSnsRequest request) {
        return SuccessResponse.ok(exhibitionHostService.updateHostSns(snsId, request), "주최 기관 SNS 수정 성공");
    }

    // SNS 삭제
    @Operation(summary = "주최기관 SNS 링크 삭제")
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/host/sns/{snsId}")
    public SuccessResponse<List<HostSnsResponse>> deleteHostSns(@PathVariable UUID snsId) {
        return SuccessResponse.ok(exhibitionHostService.deleteHostSns(snsId), "주최 기관 SNS 삭제 성공");
    }

}
