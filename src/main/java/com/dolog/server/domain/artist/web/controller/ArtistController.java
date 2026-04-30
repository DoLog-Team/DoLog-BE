package com.dolog.server.domain.artist.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "artist-기본 정보")
@RestController
@RequiredArgsConstructor
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistService artistService;

    // 작가 목록 조회
    @Operation(summary = "작가 목록 조회")
    @GetMapping
    public SuccessResponse<List<ArtistResponse>> getArtists() {
        return SuccessResponse.ok(
                artistService.getArtists(),
                "작가 목록 조회 성공"
        );
    }

    // 작가 상세 조회
    @Operation(summary = "작가 단일 조회")
    @GetMapping("/{artistId}")
    public SuccessResponse<ArtistResponse> getArtist(
            @PathVariable UUID artistId
    ) {
        return SuccessResponse.ok(
                artistService.getArtist(artistId),
                "작가 상세 조회 성공"
        );
    }
}