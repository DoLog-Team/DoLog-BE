package com.dolog.server.domain.exhibition.web.controller;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.web.dto.response.artist.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionArtistService;
import com.dolog.server.domain.exhibition.web.dto.request.artist.AddArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.request.artist.ExhibitionArtistStatusUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.artist.RemoveArtistRequest;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.dolog.server.domain.artist.web.dto.request.ArtistJoinCodeValidateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinCodeValidateResponse;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.dolog.server.domain.artist.web.dto.request.ArtistJoinRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "artist-전시참여작가")
@RestController
@RequestMapping("/exhibitions")
@RequiredArgsConstructor
public class ExhibitionArtistController {
    private final ExhibitionArtistService exhibitionArtistService;

    // 전시 참여 작가 목록 조회
    @Operation(summary = "전시 참여 작가 목록 조회")
    @GetMapping("/{exhibitionId}/artists")
    public SuccessResponse<List<ExhibitionArtistListResponse>> getArtists(
            @PathVariable UUID exhibitionId,
            @RequestParam(defaultValue = "NAME") String sort
    ) {
        return SuccessResponse.ok(
                exhibitionArtistService.getArtistsByExhibition(exhibitionId, sort),
                "전시 작가 목록 조회 성공"
        );
    }

    //전시 작가 추가
    @Operation(summary = "전시 작가 추가")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')")
    @PostMapping("/{exhibitionId}/artists")
    public ResponseEntity<SuccessResponse<ExhibitionArtistAddResponse>> addArtist(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody AddArtistRequest request
    ) {
        ExhibitionArtistAddResponse data =
                exhibitionArtistService.addArtistToExhibition(
                        user.getId(),
                        exhibitionId,
                        request.artistId()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(
                        data,
                        "전시 작가 추가 성공"
                ));
    }

    //전시 작가 제외
    @Operation(summary = "전시 작가 제외")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')")
    @DeleteMapping("/{exhibitionId}/artists")
    public SuccessResponse<ExhibitionArtistRemoveResponse> removeArtist(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody RemoveArtistRequest request
    ) {
        ExhibitionArtistRemoveResponse data =
                exhibitionArtistService.removeArtistFromExhibition(
                        user.getId(),
                        exhibitionId,
                        request.getArtistId()
                );

        return SuccessResponse.ok(data);
    }

    @Operation(summary = "작가 참여 코드 검증")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    @PostMapping("/join/validate")
    public SuccessResponse<ArtistJoinCodeValidateResponse> validateJoinCode(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ArtistJoinCodeValidateRequest request
    ) {
        ArtistJoinCodeValidateResponse data =
                exhibitionArtistService.validateJoinCode(
                        user.getId(),
                        request.joinCode()
                );

        return SuccessResponse.ok(
                data,
                "작가 참여 코드 확인 성공"
        );
    }

    @Operation(summary = "작가 전시 참여 신청")
    @PreAuthorize("hasRole('ARTIST_ADMIN')")
    @PostMapping("/join")
    public ResponseEntity<SuccessResponse<ArtistJoinResponse>> joinExhibition(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ArtistJoinRequest request
    ) {
        ArtistJoinResponse data =
                exhibitionArtistService.joinExhibition(
                        user.getId(),
                        request.joinCode(),
                        request.greeting()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.created(
                        data,
                        "전시 참여 신청 성공"
                ));
    }

    @Operation(summary = "전시 참여 작가 상태 일괄 변경")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')")
    @PatchMapping("/{exhibitionId}/artists/status")
    public SuccessResponse<ExhibitionArtistStatusUpdateResponse>
    updateArtistStatuses(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID exhibitionId,
            @Valid @RequestBody ExhibitionArtistStatusUpdateRequest request
    ) {
        ExhibitionArtistStatusUpdateResponse data =
                exhibitionArtistService.updateArtistStatuses(
                        user.getId(),
                        exhibitionId,
                        request.artistIds(),
                        request.status()
                );

        return SuccessResponse.ok(
                data,
                "전시 참여 작가 상태 변경 성공"
        );
    }

    @Operation(summary = "관리자용 전시 참여 작가 목록 조회")
    @PreAuthorize("hasAnyRole('DOLOG_ADMIN', 'EXHIBITION_ADMIN')")
    @GetMapping("/{exhibitionId}/artists/manage")
    public SuccessResponse<ExhibitionArtistManageListResponse> getArtistsForManagement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID exhibitionId,
            @RequestParam ExhibitionArtistStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ExhibitionArtistManageListResponse response =
                exhibitionArtistService.getArtistsForManagement(
                        userDetails.getId(),
                        exhibitionId,
                        status,
                        search,
                        page,
                        size
                );

        return SuccessResponse.ok(
                response,
                "전시 작가 관리 목록 조회 성공"
        );
    }
}
