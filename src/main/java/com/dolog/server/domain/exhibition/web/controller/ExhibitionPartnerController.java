package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionPartnerService;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartUpdateResponse;
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
public class ExhibitionPartnerController {

    private final ExhibitionPartnerService exhibitionPartnerService;

    // 파트 생성
    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/{exhibitionId}/partners/parts")
    public ResponseEntity<SuccessResponse<PartnerPartCreateResponse>> createPart(
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody PartnerPartCreateRequest request
    ) {
        PartnerPartCreateResponse data = exhibitionPartnerService.createPart(exhibitionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data));
    }

    // 파트 수정
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/partners/parts/{partId}")
    public SuccessResponse<PartnerPartUpdateResponse> updatePart(
            @PathVariable UUID partId,
            @RequestBody PartnerPartUpdateRequest request
    ) {
        PartnerPartUpdateResponse data = exhibitionPartnerService.updatePart(partId, request);
        return SuccessResponse.ok(data, "파트 정보가 수정되었습니다.");
    }

    // 파트 삭제
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/partners/parts/{partId}")
    public SuccessResponse<Void> deletePart(
            @PathVariable UUID partId
    ) {
        exhibitionPartnerService.deletePart(partId);
        return SuccessResponse.ok(null, "데이터 삭제에 성공하였습니다.");
    }
}
