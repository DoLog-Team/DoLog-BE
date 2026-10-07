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

    // 통합 수정용: 요청한 프로필 목록으로 작가 연결을 맞춘다. 프로필은 그 전시 것만, 새로 붙이는 작가는 참여 중(JOINED)이어야 한다.
    // 이미 연결된 작가는 제외(REMOVED)됐어도 크레딧으로 남길 수 있게 참여 상태를 보지 않는다.
    @Override
    public void syncArtistProfiles(Artwork artwork, UUID exhibitionId, List<UUID> profileIds, Map<UUID, String> artistRoles) {

        Map<UUID, String> roles = artistRoles == null ? Map.of() : artistRoles.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().trim()));
        List<UUID> distinctIds = profileIds.stream().distinct().toList();
        List<ArtistProfile> profiles = artistProfileRepository.findAllById(distinctIds);

        boolean invalid = profiles.size() != distinctIds.size()
                || profiles.stream().anyMatch(profile -> !profile.getExhibition().getId().equals(exhibitionId))
                || profiles.stream().map(profile -> profile.getArtist().getId()).distinct().count() != profiles.size()
                || profiles.stream().anyMatch(profile -> !artwork.isLinkedTo(profile.getArtist().getId()) && !isJoined(profile));
        if (invalid) {
            throw new ArtworkException(ArtworkErrorCode.CO_ARTIST_NOT_IN_EXHIBITION);
        }

        Map<UUID, ArtistProfile> profileByArtist = profiles.stream()
                .collect(Collectors.toMap(profile -> profile.getArtist().getId(), profile -> profile));

        // 빠진 작가는 연결 해제, 남은 작가는 프로필과 역할만 갱신 (지웠다 다시 넣지 않는다)
        artwork.getArtworkArtistMaps().removeIf(map -> !profileByArtist.containsKey(map.getArtist().getId()));
        artwork.getArtworkArtistMaps().forEach(map -> {
            ArtistProfile profile = profileByArtist.remove(map.getArtist().getId());
            map.linkProfile(profile);
            if (roles.get(profile.getId()) != null) {
                map.updateRole(roles.get(profile.getId()));
            }
        });
        profileByArtist.values().forEach(profile -> artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist())
                .artistProfile(profile)
                .artistRole(roles.getOrDefault(profile.getId(), ""))
                .build()));
    }
}
