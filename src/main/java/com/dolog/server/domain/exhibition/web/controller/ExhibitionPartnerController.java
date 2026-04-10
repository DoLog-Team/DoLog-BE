package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.service.ExhibitionPartnerService;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartCreateResponse;
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
}
