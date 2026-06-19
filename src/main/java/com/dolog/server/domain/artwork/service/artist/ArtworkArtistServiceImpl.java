package com.dolog.server.domain.artwork.service.artist;


import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistMappingRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkArtistServiceImpl implements ArtworkArtistService {
    private final ArtworkRepository artworkRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtworkArtistMapRepository artworkArtistMapRepository;
    private final ArtistRepository artistRepository;

    @Override
    public ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 검증 및 저장 로직
        if (!artwork.getExhibition().getId().equals(profile.getExhibition().getId())) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        ArtworkArtistMap map = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist())
                .artistProfile(profile)
                .artistRole(request.getArtistRole())
                .build();

        return ArtworkArtistMappingResponse.of(artworkArtistMapRepository.save(map));
    }

    /** 작품 ID 목록으로 작품별 작가명을 Map으로 반환
     * - 작가가 여러 명이면 ", "로 합쳐서 반환 (ex. "홍길동, 김철수")
     * - key: artworkId, value: 작가명 */
    public Map<UUID, String> fetchArtistMap(List<UUID> artworkIds) {
        // ArtworkArtistMap 조회 시 ArtistProfile도 같이 fetch join 하도록 Repository를 구성하는 것이 좋습니다.
        List<ArtworkArtistMap> artistMaps = artworkArtistMapRepository.findByArtworkIdIn(artworkIds);

        return artistMaps.stream()
                .collect(Collectors.groupingBy(
                        aam -> aam.getArtwork().getId(),
                        Collectors.mapping(aam -> {
                            // 1순위: ArtistProfile의 이름, 2순위: Artist 엔티티의 기본 이름
                            if (aam.getArtistProfile() != null && aam.getArtistProfile().getNameKo() != null) {
                                return aam.getArtistProfile().getNameKo();
                            }
                            return aam.getArtist().getNameKo();
                        }, Collectors.joining(", "))
                ));
    }

    @Override
    public ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistProfileId, ArtworkArtistMappingRequest request) {
        // profileId를 기반으로 매핑 데이터 조회
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistProfileId(artworkId, artistProfileId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_ARTIST_MAPPING_NOT_FOUND));

        // 역할 수정
        map.updateRole(request.getArtistRole());

        return ArtworkArtistMappingResponse.of(map);
    }

    @Override
    public void deleteArtistMapping(UUID artworkId, UUID artistProfileId) {
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistProfileId(artworkId, artistProfileId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_ARTIST_MAPPING_NOT_FOUND));

        artworkArtistMapRepository.delete(map);
    }

    @Override
    public void updateArtworkArtists(Artwork artwork, List<UUID> artistIds) {
        // 기존 매핑의 역할 보존 (artistId → role)
        Map<UUID, String> existingRoles = artwork.getArtworkArtistMaps().stream()
                .collect(Collectors.toMap(
                        map -> map.getArtist().getId(),
                        ArtworkArtistMap::getArtistRole,
                        (existing, duplicate) -> existing
                ));

        // 기존 매핑 비우기
        artwork.getArtworkArtistMaps().clear();

        // 새로 받은 ID들로 매핑 다시 만들기 (기존 역할 유지, 없으면 "Artist")
        List<Artist> artists = artistRepository.findAllById(artistIds);
        artists.forEach(artist -> {
            String role = existingRoles.getOrDefault(artist.getId(), "");
            artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                    .artwork(artwork)
                    .artist(artist)
                    .artistRole(role)
                    .build());
        });
    }
}
