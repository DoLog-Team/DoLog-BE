package com.dolog.server.domain.bts.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.bts.service.BtsService;
import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsMappingUpdateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsDetailResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsMappingUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "bts")
@RestController
@RequiredArgsConstructor
@RequestMapping("/exhibitions")
public class BtsController {

    private final BtsService btsService;

    // 등록 (POST)
    @Operation(summary = "BTS 등록")
    @PostMapping("/bts")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<BtsCreateResponse> createBts(
            @ModelAttribute BtsCreateRequest request
    ) throws IOException {
        BtsCreateResponse response = btsService.createBts(request);
        return SuccessResponse.ok(response, "BTS 콘텐츠 등록 성공");
    }

    // 수정 (PATCH)
    @Operation(summary = "BTS 수정")
    @PatchMapping("/bts/{btsId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<BtsCreateResponse> updateBts(
            @PathVariable UUID btsId,
            @Valid @RequestBody BtsUpdateRequest request) {

        BtsCreateResponse data = btsService.updateBts(btsId, request);
        return SuccessResponse.ok(data, "BTS 정보가 성공적으로 수정되었습니다.");
    }

    // 삭제 (DELETE)
    @Operation(summary = "BTS 삭제")
    @DeleteMapping("/bts/{btsId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<Void> deleteBts(@PathVariable UUID btsId) {
        btsService.deleteBts(btsId);
        return SuccessResponse.ok(null, "BTS 콘텐츠가 성공적으로 삭제되었습니다.");
    }


    // 조회 (GET)
    @Operation(summary = "BTS 목록 조회")
    @GetMapping("/{exhibitionId}/bts")
    public SuccessResponse<Map<String, Object>> getBtsList(@PathVariable UUID exhibitionId) {
        List<BtsListResponse> btsList = btsService.getExhibitionBtsList(exhibitionId);

        // 요청하신 JSON 형식대로 "content" 키에 담아 반환
        Map<String, Object> response = new HashMap<>();
        response.put("content", btsList);
        response.put("totalElements", btsList.size());

        return SuccessResponse.ok(response, "BTS 목록을 조회했습니다.");
    }


    // 상세 조회 (GET)
    @Operation(summary = "BTS 상세 조회")
    @GetMapping("/bts/{btsId}")
    public SuccessResponse<BtsDetailResponse> getBtsDetail(
            @PathVariable(value = "btsId") UUID btsId
    ) {
        // 서비스 호출하여 상세 데이터(작가 프로필, 연관 작품, 추천 BTS 포함) 수신
        BtsDetailResponse response = btsService.getBtsDetail(btsId);

        return SuccessResponse.ok(response, "BTS 상세 조회 성공");
    }





    // BTS 매핑 등록/수정 (PUT)
    @Operation(summary = "BTS 매핑 등록/수정")
    @PutMapping("/{exhibitionId}/bts/{btsId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<BtsMappingUpdateResponse> syncBtsMapping(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID btsId,
            @Valid @RequestBody BtsMappingUpdateRequest request) {
        BtsMappingUpdateResponse data = btsService.syncBtsMapping(exhibitionId, btsId, request);
        return SuccessResponse.ok(data, "BTS 정보 및 매핑 데이터가 성공적으로 동기화되었습니다.");
    }

}