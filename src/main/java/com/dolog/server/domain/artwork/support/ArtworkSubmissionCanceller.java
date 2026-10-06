package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 출품 취소는 엔티티를 비우는 것과 위치 지도 파일 정리가 한 쌍이라, 다른 도메인에서도 이걸 거쳐 취소한다.
@Component
@RequiredArgsConstructor
public class ArtworkSubmissionCanceller {

    private final ArtworkFileHandler artworkFileHandler;

    public void cancel(Artwork artwork) {
        artworkFileHandler.deleteAfterCommit(artwork.getLocationMap());
        artwork.cancelExhibitionSubmission();
    }
}
