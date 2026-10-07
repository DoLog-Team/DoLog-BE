package com.dolog.server.domain.artwork.service.artist;


import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistMappingRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkArtistRoleRequest;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
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

import java.util.HashMap;
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
    private final ArtworkValidator artworkValidator;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    // 공동 작가의 전시별 프로필을 연결해야 해서 출품된 작품에만 등록할 수 있다.
    // 작가는 전시 필수 항목이 아니라서 작가 연결이 바뀌어도 공개 상태는 다시 검사하지 않는다.
    @Override
    public ArtworkArtistMappingResponse createArtistMapping(
            UUID accountId, UUID artworkId, ArtworkArtistMappingRequest request) {

        Artist me = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, me);

        if (artwork.getExhibition() == null) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_SUBMITTED);
        }

        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(ArtistProfileNotFoundException::new);

        if (!profile.getExhibition().getId().equals(artwork.getExhibition().getId())
                || !isJoined(profile)) {
            throw new ArtworkException(ArtworkErrorCode.CO_ARTIST_NOT_IN_EXHIBITION);
        }

        if (artwork.isLinkedTo(profile.getArtist().getId())) {
            throw new ArtworkException(ArtworkErrorCode.CO_ARTIST_ALREADY_LINKED);
        }

        ArtworkArtistMap map = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist())
                .artistProfile(profile)
                .artistRole(request.getArtistRole().trim())
                .build();
        artwork.getArtworkArtistMaps().add(map);

        return ArtworkArtistMappingResponse.of(artworkArtistMapRepository.saveAndFlush(map));
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
    public ArtworkArtistMappingResponse updateArtistMapping(
            UUID accountId, UUID artworkId, UUID artistId, ArtworkArtistRoleRequest request) {

        ArtworkArtistMap map = getOwnedMapping(accountId, artworkId, artistId);
        map.updateRole(request.artistRole().trim());

        return ArtworkArtistMappingResponse.of(map);
    }

    // 작가 연결이 하나도 없으면 아무도 작품을 관리할 수 없어서 마지막 연결은 지우지 않는다.
    @Override
    public void deleteArtistMapping(UUID accountId, UUID artworkId, UUID artistId) {

        ArtworkArtistMap map = getOwnedMapping(accountId, artworkId, artistId);

        if (map.getArtwork().getArtworkArtistMaps().size() <= 1) {
            throw new ArtworkException(ArtworkErrorCode.LAST_ARTIST_CANNOT_BE_REMOVED);
        }

        map.getArtwork().getArtworkArtistMaps().remove(map);
    }

    private ArtworkArtistMap getOwnedMapping(UUID accountId, UUID artworkId, UUID artistId) {

        Artist me = artworkValidator.getLoginArtist(accountId);
        artworkValidator.getOwnedArtwork(artworkId, me);

        return artworkArtistMapRepository.findByArtworkIdAndArtistId(artworkId, artistId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_ARTIST_MAPPING_NOT_FOUND));
    }

    private boolean isJoined(ArtistProfile profile) {

        return exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(profile.getExhibition().getId(), profile.getArtist().getId())
                .filter(map -> map.getStatus() == ExhibitionArtistStatus.JOINED)
                .isPresent();
    }

    @Override
    public void updateArtworkArtists(Artwork artwork, List<UUID> artistIds, Map<UUID, String> artistRoles) {
        // null로 넘어올 경우 빈 Map으로 정규화
        Map<UUID, String> roles = artistRoles != null ? artistRoles : new HashMap<>();

        // 기존 매핑의 역할 보존 (artistId → role)
        Map<UUID, String> existingRoles = artwork.getArtworkArtistMaps().stream()
                .collect(Collectors.toMap(
                        map -> map.getArtist().getId(),
                        ArtworkArtistMap::getArtistRole,
                        (existing, duplicate) -> existing
                ));

        // 기존 매핑 비우기
        artwork.getArtworkArtistMaps().clear();

        // 우선순위: 요청의 artistRoles(non-null) > 기존 역할 > 빈 문자열
        List<Artist> artists = artistRepository.findAllById(artistIds);
        artists.forEach(artist -> {
            String requested = roles.get(artist.getId());
            String role = (requested != null) ? requested
                    : existingRoles.getOrDefault(artist.getId(), "");
            artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                    .artwork(artwork)
                    .artist(artist)
                    .artistRole(role)
                    .build());
        });
    }
}
