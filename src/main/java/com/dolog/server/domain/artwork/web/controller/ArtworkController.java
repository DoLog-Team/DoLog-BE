package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.artwork.service.ArtworkService;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artworks")
public class ArtworkController {

    private final ArtworkService artworkService;

    /**
     * 작품 기본 정보 등록
     * [요구사항] Developer 권한을 가진 계정만 데이터 입력 가능
     */
    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    public SuccessResponse<ArtworkCreateResponse> createArtwork(
            @Valid @RequestBody ArtworkCreateRequest request) {

        ArtworkCreateResponse data = artworkService.createArtwork(request);

        return SuccessResponse.created(data);
    }

    @PostMapping("/{artworkId}/images")
    @PreAuthorize("hasRole('DEVELOPER')") // 권한 설정 확인!
    public SuccessResponse<ArtworkImgCreateResponse> createArtworkImages(
            @PathVariable UUID artworkId,
            @RequestBody List<ArtworkImgCreateRequest> requests // 리스트 형태로 받습니다
    ) {
        ArtworkImgCreateResponse response = artworkService.createArtworkImages(artworkId, requests);

        return SuccessResponse.ok(response, "작품 상세 이미지 등록에 성공하였습니다.");
    }
}