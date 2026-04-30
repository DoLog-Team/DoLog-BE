package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionPartnerService;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberResponse;
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
@RequestMapping("/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final ExhibitionPartnerService exhibitionPartnerService;

    // 파트 수정
    @Operation(summary = "도움을 주신 분들 파트 정보 수정")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/parts/{partId}")
    public SuccessResponse<PartnerPartResponse> updatePart(
            @PathVariable UUID partId,
            @RequestBody PartnerPartUpdateRequest request
    ) {
        PartnerPartResponse data = exhibitionPartnerService.updatePart(partId, request);
        return SuccessResponse.ok(data, "파트 정보가 수정되었습니다.");
    }

    // 파트 삭제
    @Operation(summary = "도움을 주신 분들 파트 삭제")
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/parts/{partId}")
    public SuccessResponse<Void> deletePart(
            @PathVariable UUID partId
    ) {
        exhibitionPartnerService.deletePart(partId);
        return SuccessResponse.ok(null, "데이터 삭제에 성공하였습니다.");
    }

    // 멤버 등록
    @Operation(summary = "도움을 주신 분들 멤버 등록")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/parts/{partId}/members")
    public ResponseEntity<SuccessResponse<PartnerMemberResponse>> createMember(
            @PathVariable UUID partId,
            @Valid @RequestBody PartnerMemberCreateRequest request
    ) {
        PartnerMemberResponse data = exhibitionPartnerService.createMember(partId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data));
    }

    // 멤버 수정
    @Operation(summary = "도움을 주신 분들 멤버 수정")
    @PreAuthorize("hasRole('DEVELOPER')")
    @PatchMapping("/members/{memberId}")
    public SuccessResponse<PartnerMemberResponse> updateMember(
            @PathVariable UUID memberId,
            @RequestBody PartnerMemberUpdateRequest request
    ) {
        PartnerMemberResponse data = exhibitionPartnerService.updateMember(memberId, request);
        return SuccessResponse.ok(data, "멤버 정보가 성공적으로 수정되었습니다.");
    }

    // 멤버 삭제
    @Operation(summary = "도움을 주신 분들 멤버 삭제")
    @PreAuthorize("hasRole('DEVELOPER')")
    @DeleteMapping("/members/{memberId}")
    public SuccessResponse<Void> deleteMember(
            @PathVariable UUID memberId
    ) {
        exhibitionPartnerService.deleteMember(memberId);
        return SuccessResponse.ok(null, "데이터 삭제에 성공하였습니다.");
    }
}
