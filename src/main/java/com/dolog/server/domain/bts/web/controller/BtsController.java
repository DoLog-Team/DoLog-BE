package com.dolog.server.domain.bts.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import com.dolog.server.domain.bts.service.BtsService;
import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsMappingUpdateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsDetailResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListWrapperResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsMappingUpdateResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;
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
    @PostMapping(value = "/{exhibitionId}/bts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<BtsResponse> createBts(
            @PathVariable UUID exhibitionId,
            @Valid @ParameterObject @ModelAttribute BtsCreateRequest request,
            @RequestParam(required = false) MultipartFile mainImg
    ) throws IOException {
        BtsResponse response = btsService.createBts(exhibitionId, request, mainImg);
        return SuccessResponse.ok(response, "BTS 콘텐츠 등록 성공");
    }

    // 수정 (PATCH)
    @Operation(summary = "BTS 수정")
    @PatchMapping(value = "/{exhibitionId}/bts/{btsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<BtsResponse> updateBts(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID btsId,
            @ParameterObject @ModelAttribute BtsUpdateRequest request,
            @RequestParam(required = false) MultipartFile mainImg) throws IOException {

        BtsResponse data = btsService.updateBts(exhibitionId, btsId, request, mainImg);
        return SuccessResponse.ok(data, "BTS 정보가 성공적으로 수정되었습니다.");
    }

    // 삭제 (DELETE)
    @Operation(summary = "BTS 삭제")
    @DeleteMapping("/{exhibitionId}/bts/{btsId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<Void> deleteBts(@PathVariable UUID exhibitionId, @PathVariable UUID btsId) {
        btsService.deleteBts(exhibitionId, btsId);
        return SuccessResponse.ok(null, "BTS 콘텐츠가 성공적으로 삭제되었습니다.");
    }


    // 조회 (GET)
    @Operation(summary = "BTS 목록 조회")
    @GetMapping("/{exhibitionId}/bts")
    public SuccessResponse<BtsListWrapperResponse> getBtsList(@PathVariable UUID exhibitionId) {
        List<BtsListResponse> btsList = btsService.getExhibitionBtsList(exhibitionId);
        return SuccessResponse.ok(new BtsListWrapperResponse(btsList, btsList.size()), "BTS 목록을 조회했습니다.");
    }


    // 상세 조회 (GET)
    @Operation(summary = "BTS 상세 조회")
    @GetMapping("/{exhibitionId}/bts/{btsId}")
    public SuccessResponse<BtsDetailResponse> getBtsDetail(
            @PathVariable UUID exhibitionId,
            @PathVariable(value = "btsId") UUID btsId
    ) {
        // 서비스 호출하여 상세 데이터(작가 프로필, 연관 작품, 추천 BTS 포함) 수신
        BtsDetailResponse response = btsService.getBtsDetail(exhibitionId, btsId);

        return SuccessResponse.ok(response, "BTS 상세 조회 성공");
    }





    // BTS 매핑 등록/수정 (PUT)
    @Operation(summary = "BTS 매핑 등록/수정")
    @PutMapping("/{exhibitionId}/bts/{btsId}")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<BtsMappingUpdateResponse> syncBtsMapping(
            @PathVariable UUID exhibitionId,
            @PathVariable UUID btsId,
            @Valid @RequestBody BtsMappingUpdateRequest request) {
        BtsMappingUpdateResponse data = btsService.syncBtsMapping(exhibitionId, btsId, request);
        return SuccessResponse.ok(data, "BTS 정보 및 매핑 데이터가 성공적으로 동기화되었습니다.");
    }

}