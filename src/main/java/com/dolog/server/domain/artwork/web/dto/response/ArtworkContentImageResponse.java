package com.dolog.server.domain.artwork.web.dto.response;

// 본문(마크다운)에 인라인으로 삽입할 이미지 업로드 결과. DB 에는 아무것도 남기지 않는다.
public record ArtworkContentImageResponse(String imageUrl) {
}
