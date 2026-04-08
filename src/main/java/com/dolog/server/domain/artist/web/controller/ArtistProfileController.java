package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}