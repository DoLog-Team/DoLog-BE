package com.dolog.server.domain.artwork.service.view;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 조회수는 상세 조회가 아니라, 이용자가 상세 페이지에 머문 뒤 브라우저가 보내는 요청으로 센다.
// 같은 방문자(기기)는 작품마다 한 번만 센다. 작가 본인도 센다.
@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkViewService {

    private final ArtworkRepository artworkRepository;

    public ArtworkViewResponse recordView(UUID artworkId, String visitorId) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .filter(Artwork::isVisibleOnDolog)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        boolean counted = artworkRepository.insertViewLogIfAbsent(artworkId, visitorId) == 1;
        if (counted) {
            artworkRepository.increaseViewCount(artworkId);
        }

        long viewCount = artwork.getViewCount() + (counted ? 1 : 0);
        return new ArtworkViewResponse(artworkId, viewCount, counted, visitorId);
    }
}
