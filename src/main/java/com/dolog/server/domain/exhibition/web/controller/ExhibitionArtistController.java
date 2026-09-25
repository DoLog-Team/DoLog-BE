package com.dolog.server.domain.exhibition.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.exhibition.service.ExhibitionArtistService;
import com.dolog.server.domain.exhibition.web.dto.request.artist.AddArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.request.artist.RemoveArtistRequest;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistRemoveResponse;
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
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @PostMapping("/{exhibitionId}/artists")
    public SuccessResponse<ExhibitionArtistAddResponse> addArtist(
            @PathVariable UUID exhibitionId,
            @RequestBody AddArtistRequest request
    ) {
        ExhibitionArtistAddResponse data =
                exhibitionArtistService.addArtistToExhibition(exhibitionId, request.getArtistId());

        return SuccessResponse.ok(
                data,
                "작가가 전시에 추가되었습니다."
        );
    }

    //전시 작가 삭제
    @Operation(summary = "전시 작가 삭제")
    @PreAuthorize("hasRole('DOLOG_ADMIN')")
    @DeleteMapping("/{exhibitionId}/artists")
    public SuccessResponse<ExhibitionArtistRemoveResponse> removeArtist(
            @PathVariable UUID exhibitionId,
            @RequestBody RemoveArtistRequest request
    ) {
        ExhibitionArtistRemoveResponse data =
                exhibitionArtistService.removeArtistFromExhibition(exhibitionId, request.getArtistId());

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
}
