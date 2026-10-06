package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.bts.repository.BtsArtworkMapRepository;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkDeleteProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkArtistMapRepository artistMaps;
    private final BtsArtworkMapRepository btsMaps;
    private final ArtworkLikeRepository artworkLikeRepository;

    public void delete(
            UUID accountId,
            UUID artworkId
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        // 작품과 작가·BTS 연결은 보관 기간 동안 숨겨 두고, 좋아요는 바로 지운다.
        LocalDateTime now = LocalDateTime.now();
        artwork.markDeleted(now);
        artistMaps.hideByArtworkId(artworkId, now);
        btsMaps.hideByArtworkId(artworkId, now);
        // 컨텍스트를 비우기 전에 위 변경을 flush 하도록 마지막에 호출한다.
        artworkLikeRepository.deleteAllByArtworkId(artworkId);
    }
}
