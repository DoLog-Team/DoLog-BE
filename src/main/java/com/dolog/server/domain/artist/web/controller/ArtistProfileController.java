package com.dolog.server.domain.artist.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artist.service.ArtistProfileService;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsUpdateResponse;
import com.dolog.server.domain.like.support.VisitorIdResolver;
import com.dolog.server.global.response.SuccessResponse;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artist-전시참여작가")
@RestController
@RequestMapping("artist-profiles")
@RequiredArgsConstructor
public class ArtistProfileController {

    private final ArtistProfileService artistProfileService;
    private final VisitorIdResolver visitorIdResolver;

    @Operation(summary = "관리자용 전시 작가 프로필 목록 조회")
    @GetMapping
    @PreAuthorize(
            "hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')"
    )
    public SuccessResponse<ArtistProfileListResponse> getArtistProfileList(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam UUID exhibitionId
    ) {
        ArtistProfileListResponse response =
                artistProfileService.getArtistProfileList(
                        user.getId(),
                        exhibitionId
                );

        return SuccessResponse.ok(response, "프로필 목록 조회 성공");
    }

    // 프로필 상세 조회
    @Operation(summary = "전시 작가 프로필 상세 조회")
    @GetMapping("/{profileId}")
    public SuccessResponse<ArtistProfileDetailResponse> getArtistProfileDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest request,
            @PathVariable UUID profileId
    ) {
        UUID accountId = user == null ? null : user.getId();
        String visitorId = visitorIdResolver.resolve(request, null)
                .orElse(null);
        ArtistProfileDetailResponse response =
                artistProfileService.getArtistProfileDetail(
                        accountId,
                        profileId,
                        visitorId
                );
        return SuccessResponse.ok(response, "프로필 상세 조회 성공");
    }


    // 프로필 생성
    @Operation(summary = "전시 작가 프로필 생성")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public ResponseEntity<SuccessResponse<ArtistProfileCreateResponse>>
    createArtistProfile(
            @Valid
            @RequestPart("request")
            ArtistProfileCreateRequest request,

            @RequestPart(
                    value = "profileImg",
                    required = false
            )
            MultipartFile profileImg
    ) throws IOException {

        ArtistProfileCreateResponse response =
                artistProfileService.createArtistProfile(
                        request,
                        profileImg
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(
                        response,
                        "프로필 등록 성공"
                ));
    }

    // 프로필 수정
    @Operation(summary = "전시 작가 프로필 수정")
    @PatchMapping(
            value = "/{profileId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('ARTIST_ADMIN', 'EXHIBITION_ADMIN', 'DOLOG_ADMIN')"
    )
    public SuccessResponse<ArtistProfileUpdateResponse> updateArtistProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID profileId,
            @Valid
            @RequestPart("request")
            ArtistProfileUpdateRequest request,

            @RequestPart(
                    value = "profileImg",
                    required = false
            )
            MultipartFile profileImg
    ) throws IOException {
        ArtistProfileUpdateResponse response =
                artistProfileService.updateArtistProfile(
                        user.getId(),
                        profileId,
                        request,
                        profileImg
                );

        return SuccessResponse.ok(response, "프로필 수정 성공");
    }


//    -------------------------[ SNS ]-----------------------------------


    // SNS 추가
    // POST artist-profiles/{profileId}/sns
    @Operation(summary = "작가 SNS 추가")
    @PostMapping("/{profileId}/sns")
    @PreAuthorize(
            "hasAnyRole('ARTIST_ADMIN', 'EXHIBITION_ADMIN', 'DOLOG_ADMIN')"
    )
    public ResponseEntity<SuccessResponse<ArtistSnsCreateResponse>> addArtistSns(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID profileId,
            @Valid @RequestBody ArtistSnsRequest request
    ) {

        ArtistSnsCreateResponse response =
                artistProfileService.addArtistSns(
                        user.getId(),
                        profileId,
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(
                        response,
                        "SNS 추가 성공"
                ));
    }

    // SNS 삭제
    // DELETE artist-profiles/sns/{snsId}
    @Operation(summary = "작가 SNS 삭제")
    @DeleteMapping("/sns/{snsId}")
    @PreAuthorize(
            "hasAnyRole('ARTIST_ADMIN', 'EXHIBITION_ADMIN', 'DOLOG_ADMIN')"
    )
    public SuccessResponse<Void> deleteArtistSns(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID snsId
    ) {
        artistProfileService.deleteArtistSns(user.getId(), snsId);

        return SuccessResponse.ok(null, "SNS 삭제 성공");
    }

    // SNS 목록 조회
    // GET artist-profiles/{profileId}/sns
    @Operation(summary = "작가 SNS 목록 조회")
    @GetMapping("/{profileId}/sns")
    public SuccessResponse<ArtistSnsListResponse> getArtistSnsList(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID profileId
    ) {

        UUID accountId = user == null ? null : user.getId();
        ArtistSnsListResponse response =
                artistProfileService.getArtistSnsList(
                        accountId,
                        profileId
                );

        return SuccessResponse.ok(response, "SNS 목록 조회 성공");
    }

    // SNS 수정
    // PATCH artist-profiles/sns/{snsId}
    @Operation(summary = "작가 SNS 수정")
    @PatchMapping("/sns/{snsId}")
    @PreAuthorize(
            "hasAnyRole('ARTIST_ADMIN', 'EXHIBITION_ADMIN', 'DOLOG_ADMIN')"
    )
    public SuccessResponse<ArtistSnsUpdateResponse> updateArtistSns(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID snsId,
            @Valid @RequestBody ArtistSnsUpdateRequest request
    ) {
        ArtistSnsUpdateResponse response =
                artistProfileService.updateArtistSns(
                        user.getId(),
                        snsId,
                        request
                );

        return SuccessResponse.ok(response, "SNS 수정 성공");
    }
}
