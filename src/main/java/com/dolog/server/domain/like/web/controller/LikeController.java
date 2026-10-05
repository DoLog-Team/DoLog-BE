package com.dolog.server.domain.like.web.controller;

import com.dolog.server.domain.like.exception.LikeErrorCode;
import com.dolog.server.domain.like.exception.LikeException;
import com.dolog.server.domain.like.service.LikeService;
import com.dolog.server.domain.like.support.VisitorIdResolver;
import com.dolog.server.domain.like.web.dto.request.LikeRequest;
import com.dolog.server.domain.like.web.dto.response.ArtistProfileLikeResponse;
import com.dolog.server.domain.like.web.dto.response.ArtworkLikeResponse;
import com.dolog.server.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "like")
@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final VisitorIdResolver visitorIdResolver;

    @Operation(summary = "작품 좋아요", description = "비로그인 가능. visitor_id 쿠키가 없으면 본문의 visitorId, 둘 다 없으면 새로 발급해 쿠키와 응답으로 내려줍니다.")
    @PostMapping("/artworks/{artworkId}/likes")
    public ResponseEntity<SuccessResponse<ArtworkLikeResponse>> likeArtwork(
            @PathVariable UUID artworkId,
            @RequestBody(required = false) LikeRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String visitorId = visitorIdResolver.resolveOrIssue(request, response, visitorIdOf(body));
        ArtworkLikeResponse data = likeService.likeArtwork(artworkId, visitorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data, "좋아요가 등록되었습니다."));
    }

    @Operation(summary = "작품 좋아요 취소", description = "visitor_id 쿠키, 없으면 본문의 visitorId 기준으로 취소합니다.")
    @DeleteMapping("/artworks/{artworkId}/likes")
    public SuccessResponse<ArtworkLikeResponse> cancelArtworkLike(
            @PathVariable UUID artworkId,
            @RequestBody(required = false) LikeRequest body,
            HttpServletRequest request) {
        String visitorId = requireVisitorId(request, body);
        return SuccessResponse.ok(likeService.cancelArtworkLike(artworkId, visitorId), "좋아요가 취소되었습니다.");
    }

    @Operation(summary = "작가 좋아요", description = "비로그인 가능. 참여 중인 작가의 전시별 프로필만 가능합니다.")
    @PostMapping("/artist-profiles/{profileId}/likes")
    public ResponseEntity<SuccessResponse<ArtistProfileLikeResponse>> likeArtistProfile(
            @PathVariable UUID profileId,
            @RequestBody(required = false) LikeRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String visitorId = visitorIdResolver.resolveOrIssue(request, response, visitorIdOf(body));
        ArtistProfileLikeResponse data = likeService.likeArtistProfile(profileId, visitorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(data, "작가 좋아요 성공"));
    }

    @Operation(summary = "작가 좋아요 취소")
    @DeleteMapping("/artist-profiles/{profileId}/likes")
    public SuccessResponse<ArtistProfileLikeResponse> cancelArtistProfileLike(
            @PathVariable UUID profileId,
            @RequestBody(required = false) LikeRequest body,
            HttpServletRequest request) {
        String visitorId = requireVisitorId(request, body);
        return SuccessResponse.ok(likeService.cancelArtistProfileLike(profileId, visitorId), "작가 좋아요 취소 성공");
    }

    private String visitorIdOf(LikeRequest body) {
        return body != null ? body.visitorId() : null;
    }

    // 식별자가 없는 취소 요청은 취소할 좋아요가 있을 수 없다.
    private String requireVisitorId(HttpServletRequest request, LikeRequest body) {
        return visitorIdResolver.resolve(request, visitorIdOf(body))
                .orElseThrow(() -> new LikeException(LikeErrorCode.LIKE_NOT_FOUND));
    }
}
