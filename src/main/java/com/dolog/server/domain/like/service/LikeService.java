package com.dolog.server.domain.like.service;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.like.entity.ArtistProfileLike;
import com.dolog.server.domain.like.entity.ArtworkLike;
import com.dolog.server.domain.like.exception.LikeErrorCode;
import com.dolog.server.domain.like.exception.LikeException;
import com.dolog.server.domain.like.repository.ArtistProfileLikeRepository;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
import com.dolog.server.domain.like.web.dto.response.ArtistProfileLikeResponse;
import com.dolog.server.domain.like.web.dto.response.ArtworkLikeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkLikeRepository artworkLikeRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistProfileLikeRepository artistProfileLikeRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    // 두록 URL 에서도 보이는(PUBLISHED) 작품만 좋아요를 받는다.
    public ArtworkLikeResponse likeArtwork(UUID artworkId, String visitorId) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .filter(Artwork::isVisibleOnDolog)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        if (artworkLikeRepository.existsByArtworkIdAndVisitorId(artworkId, visitorId)) {
            throw new LikeException(LikeErrorCode.ALREADY_LIKED);
        }

        saveOrConflict(() -> artworkLikeRepository.saveAndFlush(
                ArtworkLike.builder().artwork(artwork).visitorId(visitorId).build()));

        return new ArtworkLikeResponse(artworkId, visitorId, artworkLikeRepository.countByArtworkId(artworkId), true);
    }

    // 작품이 비공개가 됐어도 내가 누른 좋아요는 취소할 수 있다.
    public ArtworkLikeResponse cancelArtworkLike(UUID artworkId, String visitorId) {

        ArtworkLike like = artworkLikeRepository.findByArtworkIdAndVisitorId(artworkId, visitorId)
                .orElseThrow(() -> new LikeException(LikeErrorCode.LIKE_NOT_FOUND));

        artworkLikeRepository.delete(like);
        artworkLikeRepository.flush();

        return new ArtworkLikeResponse(artworkId, visitorId, artworkLikeRepository.countByArtworkId(artworkId), false);
    }

    // 공개 프로필 기준(지우 파트)과 같이, 그 전시에 참여 중(JOINED)인 작가의 프로필만 좋아요를 받는다.
    public ArtistProfileLikeResponse likeArtistProfile(UUID profileId, String visitorId) {

        ArtistProfile profile = artistProfileRepository.findById(profileId)
                .filter(this::isJoined)
                .orElseThrow(ArtistProfileNotFoundException::new);

        if (artistProfileLikeRepository.existsByArtistProfileIdAndVisitorId(profileId, visitorId)) {
            throw new LikeException(LikeErrorCode.ALREADY_LIKED);
        }

        saveOrConflict(() -> artistProfileLikeRepository.saveAndFlush(
                ArtistProfileLike.builder().artistProfile(profile).visitorId(visitorId).build()));

        return new ArtistProfileLikeResponse(profileId, visitorId,
                artistProfileLikeRepository.countByArtistProfileId(profileId), true);
    }

    public ArtistProfileLikeResponse cancelArtistProfileLike(UUID profileId, String visitorId) {

        ArtistProfileLike like = artistProfileLikeRepository.findByArtistProfileIdAndVisitorId(profileId, visitorId)
                .orElseThrow(() -> new LikeException(LikeErrorCode.LIKE_NOT_FOUND));

        artistProfileLikeRepository.delete(like);
        artistProfileLikeRepository.flush();

        return new ArtistProfileLikeResponse(profileId, visitorId,
                artistProfileLikeRepository.countByArtistProfileId(profileId), false);
    }

    private boolean isJoined(ArtistProfile profile) {

        return exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(profile.getExhibition().getId(), profile.getArtist().getId())
                .filter(map -> map.getStatus() == ExhibitionArtistStatus.JOINED)
                .isPresent();
    }

    // 동시에 같은 좋아요가 들어오면 UNIQUE 제약에서 걸리므로 409 로 맞춘다.
    private void saveOrConflict(Runnable save) {

        try {
            save.run();
        } catch (DataIntegrityViolationException e) {
            throw new LikeException(LikeErrorCode.ALREADY_LIKED);
        }
    }
}
