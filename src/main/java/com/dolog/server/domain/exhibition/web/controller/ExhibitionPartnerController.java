package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionPartnerService;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "exhibition-partner")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionPartnerController {

    private final ExhibitionPartnerService exhibitionPartnerService;

    // 도움을 주신 분들 목록 조회
    @Operation(summary = "도움을 주신 분들 목록 조회")
    @GetMapping("/{exhibitionId}/partners")
    public SuccessResponse<PartnerListResponse> getPartners(
            @PathVariable UUID exhibitionId,
            @RequestParam(required = false) String sort
    ) {
        PartnerListResponse data = exhibitionPartnerService.getPartners(exhibitionId, sort);
        return SuccessResponse.ok(data, "도움을 주신 분들 목록 조회가 완료되었습니다.");
    }

    // 파트 생성
    @Operation(summary = "도움을 주신 분들 파트 생성")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping("/{exhibitionId}/partners/parts")
    public ResponseEntity<SuccessResponse<PartnerPartResponse>> createPart(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody PartnerPartCreateRequest request
    ) {
        PartnerPartResponse data = exhibitionPartnerService.createPart(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data, "새로운 파트가 생성되었습니다."));
    }
}
