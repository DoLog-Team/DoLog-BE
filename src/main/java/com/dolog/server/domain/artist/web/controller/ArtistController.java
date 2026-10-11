package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistPublicResponse;
import com.dolog.server.domain.like.support.VisitorIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistUpdateResponse;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "artist-기본 정보")
@RestController
@RequiredArgsConstructor
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistService artistService;
    private final VisitorIdResolver visitorIdResolver;

    @Operation(summary = "내 작가 기본정보 수정")
    @PatchMapping("/me")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    public SuccessResponse<ArtistUpdateResponse> updateMyArtist(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid ArtistUpdateRequest request
    ) {
        return SuccessResponse.ok(
                artistService.updateMyArtist(user.getId(), request), "작가 수정 성공"
        );
    }

    // 작가 목록 조회
    @Operation(summary = "작가 목록 조회")
    @GetMapping
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    public SuccessResponse<ArtistListResponse> getArtists(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return SuccessResponse.ok(
                artistService.getArtists(search, page, size),
                "작가 목록 조회 성공"
        );
    }

    // 작가 상세 조회
    @Operation(summary = "작가 단일 조회")
    @GetMapping("/{artistId}")
    public SuccessResponse<ArtistPublicResponse> getArtist(
            @PathVariable UUID artistId,
            HttpServletRequest request
    ) {
        String visitorId = visitorIdResolver.resolve(request, null)
                .orElse(null);

        return SuccessResponse.ok(
                artistService.getArtist(artistId, visitorId),
                "작가 상세 조회 성공"
        );
    }
}
