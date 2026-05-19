package com.dolog.server.domain.artist.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artist-전시참여작가")
@RestController
@RequestMapping("artist-profiles")
@RequiredArgsConstructor
public class ArtistProfileController {

    private final ArtistProfileService artistProfileService;

    /**
     * 작가 프로필 목록 조회 (전체 조회 및 전시회별 필터링)
     * GET exhibitions/artists-profiles
     * GET exhibitions/artists-profiles?exhibitionId={uuid}
     * * GET exhibitions/artists-profiles?artistProfileId={uuid}
     */
    @Operation(summary = "전시 작가(프로필) 목록 조회")
    @GetMapping
    public SuccessResponse<ArtistProfileListResponse> getArtistProfileList(
            @RequestParam(value = "exhibitionId", required = false) String exhibitionIdStr
    ) {
        UUID exhibitionId = null;

        // "null" 문자열이 들어오거나 비어있는 경우를 방어
        if (exhibitionIdStr != null && !exhibitionIdStr.isBlank() && !exhibitionIdStr.equals("null")) {
            exhibitionId = UUID.fromString(exhibitionIdStr);
        }

        // 서비스 호출 (서비스는 UUID를 받도록 유지)
        List<ArtistProfileResponse> responses = artistProfileService.getArtistProfileList(exhibitionId);

        // 데이터 포장
        ArtistProfileListResponse data = ArtistProfileListResponse.builder()
                .total(responses.size())
                .artistProfiles(responses)
                .build();

        return SuccessResponse.ok(data, "작가 프로필 목록 조회 성공");
    }

    // 프로필 상세 조회
    @Operation(summary = "전시 작가 프로필 상세 조회")
    @GetMapping("/{profileId}")
    public SuccessResponse<ArtistProfileDetailResponse> getArtistProfileDetail(
            @PathVariable(value = "profileId") String profileIdStr
    ) {
        // PathVariable도 "null" 문자열이 들어올 경우를 대비해 안전하게 처리
        if (profileIdStr == null || profileIdStr.isBlank() || profileIdStr.equals("null")) {
            throw new IllegalArgumentException("유효하지 않은 프로필 ID입니다.");
        }

        UUID profileId = UUID.fromString(profileIdStr);
        ArtistProfileDetailResponse response = artistProfileService.getArtistProfileDetail(profileId);

        return SuccessResponse.ok(response, "작가 프로필 상세 조회 성공");
    }


    // 프로필 생성
    @Operation(summary = "전시 작가 프로필 생성")
    @PostMapping(
            consumes = "multipart/form-data"
    )
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtistProfileResponse> createArtistProfile(
            @ModelAttribute ArtistProfileCreateRequest request
    ) throws Exception {

        ArtistProfileResponse response = artistProfileService.createArtistProfile(
                request.getExhibitionId(), request
        );
        return SuccessResponse.ok(response, "작가 프로필 등록 성공");
    }

    // 프로필 수정
    @Operation(summary = "전시 작가 프로필 수정")
    @PatchMapping(value = "/{profileId}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtistProfileResponse> updateArtistProfile(
            @PathVariable String profileId,
            @ModelAttribute ArtistProfileCreateRequest request
    ) throws Exception {
        ArtistProfileResponse response = artistProfileService.updateArtistProfile(profileId, request);
        return SuccessResponse.ok(response, "작가 프로필 수정 성공");
    }


//    -------------------------[ SNS ]-----------------------------------


    // SNS 추가
    // POST artist-profiles/{profileId}/sns
    @Operation(summary = "작가 SNS 추가")
    @PostMapping("/{profileId}/sns")
    public SuccessResponse<ArtistSnsResponse> addArtistSns(
            @PathVariable String profileId,
            @RequestBody ArtistSnsRequest request) throws IOException {

        ArtistSnsResponse response = artistProfileService.addArtistSns(profileId, request);
        return SuccessResponse.ok(response, "작가 SNS 등록 성공");
    }

    // SNS 삭제
    // DELETE artist-profiles/sns/{snsId}
    @Operation(summary = "작가 SNS 삭제")
    @DeleteMapping("/sns/{snsId}")
    public SuccessResponse<List<ArtistSnsResponse>> deleteArtistSns(@PathVariable UUID snsId)
            throws IOException {
        // 서비스에서 삭제 후 최신 목록을 받아옴
        List<ArtistSnsResponse> response = artistProfileService.deleteArtistSns(snsId);

        return SuccessResponse.ok(response, "작가 SNS 삭제 성공");
    }

    // SNS 목록 조회
    // GET artist-profiles/{profileId}/sns
    @Operation(summary = "작가 SNS 목록 조회")
    @GetMapping("/{profileId}/sns")
    public SuccessResponse<List<ArtistSnsResponse>> getArtistSnsList(
            @PathVariable String profileId) {

        List<ArtistSnsResponse> response = artistProfileService.getArtistSnsList(profileId);

        return SuccessResponse.ok(response, "작가 SNS 목록 조회 성공");
    }
}