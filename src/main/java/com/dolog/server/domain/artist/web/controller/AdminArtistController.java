package com.dolog.server.domain.artist.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "artist-기본 정보")
@RestController
@RequiredArgsConstructor

@RequestMapping("/admin/artists")
public class AdminArtistController {

    private final ArtistService artistService;

    //작가 생성
    @Operation(summary = "작가 생성")
    @PostMapping
    public SuccessResponse<ArtistResponse> createArtist(
            @RequestBody @Valid ArtistCreateRequest request
    ) {
        ArtistResponse data = artistService.createArtist(request);
        return SuccessResponse.created(data);
    }

    // 작가 수정
    @Operation(summary = "작가 수정")
    @PatchMapping("/{artistId}")
    public SuccessResponse<ArtistResponse> updateArtist(
            @PathVariable UUID artistId,
            @RequestBody ArtistUpdateRequest request
    ) {
        ArtistResponse data = artistService.updateArtist(artistId, request);
        return SuccessResponse.ok(data);
    }

    //작가 삭제
    @Operation(summary = "작가 삭제")
    @DeleteMapping("/{artistId}")
    public SuccessResponse<ArtistResponse> deleteArtist(
            @PathVariable UUID artistId
    ) {
        return SuccessResponse.ok(
                artistService.deleteArtist(artistId),
                "작가가 삭제되었습니다."
        );
    }
}
