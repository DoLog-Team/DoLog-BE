package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("artist-profiles")
@RequiredArgsConstructor
public class ArtistProfileController {

    private final ArtistProfileService artistProfileService;

    @PostMapping
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
    @PatchMapping("/{profileId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtistProfileResponse> updateArtistProfile(
            @PathVariable String profileId,
            @ModelAttribute ArtistProfileCreateRequest request
    ) throws Exception {
        // 서비스 로직에서 null/blank 체크를 통해 전달된 필드만 수정하도록 구현됨
        ArtistProfileResponse response = artistProfileService.updateArtistProfile(profileId, request);
        return SuccessResponse.ok(response, "작가 프로필 수정 성공");
    }


//    -------------------------[ SNS ]-----------------------------------


    // SNS 추가
    // POST artist-profiles/{profileId}/sns
    @PostMapping("/{profileId}/sns")
    public SuccessResponse<ArtistSnsResponse> addArtistSns(
            @PathVariable String profileId,
            @RequestBody ArtistSnsRequest request) throws IOException {

        ArtistSnsResponse response = artistProfileService.addArtistSns(profileId, request);
        return SuccessResponse.ok(response, "작가 SNS 등록 성공");
    }

    // SNS 삭제
    // DELETE artist-profiles/sns/{snsId}
    @DeleteMapping("/sns/{snsId}")
    public SuccessResponse<List<ArtistSnsResponse>> deleteArtistSns(@PathVariable UUID snsId)
            throws IOException {
        // 서비스에서 삭제 후 최신 목록을 받아옴
        List<ArtistSnsResponse> response = artistProfileService.deleteArtistSns(snsId);

        return SuccessResponse.ok(response, "작가 SNS 삭제 성공");
    }

    // SNS 목록 조회
    // GET artist-profiles/{profileId}/sns
    @GetMapping("/{profileId}/sns")
    public SuccessResponse<List<ArtistSnsResponse>> getArtistSnsList(
            @PathVariable String profileId) {

        List<ArtistSnsResponse> response = artistProfileService.getArtistSnsList(profileId);

        return SuccessResponse.ok(response, "작가 SNS 목록 조회 성공");
    }
}