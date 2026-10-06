package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistItemResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistStatusUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinCodeValidateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinResponse;
import com.dolog.server.global.exception.jwt.JwtInvalidException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionArtistServiceImpl implements ExhibitionArtistService{


    private final ArtistRepository artistRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final AccountRepository accountRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtworkRepository artworkRepository;

    // 전시 작가 추가
    @Override
    public ExhibitionArtistAddResponse addArtistToExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        requireCanManageArtists(actor, exhibition);

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        ExhibitionArtistMap map = exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(exhibitionId, artistId)
                .orElse(null);

        if (map != null && !map.getStatus().canBeAddedByAdmin()) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_ALREADY_EXISTS
            );
        }

        if (map == null) {
            map = exhibitionArtistMapRepository.save(
                    ExhibitionArtistMap.builder()
                            .exhibition(exhibition)
                            .artist(artist)
                            .status(ExhibitionArtistStatus.JOINED)
                            .build()
            );
        } else {
            // 기존 참여 이력은 UNIQUE 제약 때문에 새 행을 만들지 않고 복구한다.
            map.updateStatus(ExhibitionArtistStatus.JOINED);
        }

        createMissingArtistProfiles(exhibition, List.of(map));

        return ExhibitionArtistAddResponse.from(map);
    }

    private void requireCanManageArtists(
            Account actor,
            Exhibition exhibition
    ) {
        if (actor.getRole() == Role.DOLOG_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.EXHIBITION_ADMIN
                && exhibition.getAccount().getId().equals(actor.getId())) {
            return;
        }

        throw new AccessDeniedException(
                "자신이 관리하는 전시의 작가만 관리할 수 있습니다."
        );
    }

    // 전시 작가 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public ExhibitionArtistListResponse getArtistsByExhibition(
            UUID exhibitionId,
            String sort
    ) {

        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        List<ExhibitionArtistItemResponse> artists =
                exhibitionArtistMapRepository.findArtists(exhibitionId);

        // 랜덤 정렬
        if ("RANDOM".equalsIgnoreCase(sort)) {
            Collections.shuffle(artists);
            return ExhibitionArtistListResponse.from(artists);
        }

        // 기본: 가나다순
        artists.sort(
                Comparator.comparing(
                        ExhibitionArtistItemResponse::getNameKo,
                        Comparator.nullsLast(String::compareTo)
                )
        );

        return ExhibitionArtistListResponse.from(artists);
    }

    // 전시 작가 제외 (참여 이력은 삭제하지 않고 REMOVED로 전환)
    @Override
    public void removeArtistFromExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));

        requireCanManageArtists(actor, exhibition);

        ExhibitionArtistMap map = exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(exhibitionId, artistId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_FOUND
                ));

        requireStatusTransition(map, ExhibitionArtistStatus.REMOVED);
        map.updateStatus(ExhibitionArtistStatus.REMOVED);
        cancelArtworkSubmissions(exhibitionId, List.of(artistId));
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistJoinCodeValidateResponse validateJoinCode(
            UUID accountId,
            String rawCode
    ) {
        JoinContext context =
                resolveJoinContext(accountId, rawCode);

        Exhibition exhibition = context.exhibition();

        if (exhibition.getExhibitionDetail() == null) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND
            );
        }

        return new ArtistJoinCodeValidateResponse(
                exhibition.getId(),
                exhibition.getExhibitionDetail().getTitle(),
                // TODO: 자기소개 양식 정책 확정 후 고정 문구 또는 전시별 설정값으로 교체
                null
        );
    }

    @Override
    public ArtistJoinResponse joinExhibition(
            UUID accountId,
            String rawCode,
            String rawGreeting
    ) {
        JoinContext context =
                resolveJoinContext(accountId, rawCode);

        String greeting = rawGreeting.trim();

        ExhibitionArtistMap map = context.existingMap();

        if (map == null) {
            map = ExhibitionArtistMap.builder()
                    .exhibition(context.exhibition())
                    .artist(context.artist())
                    .status(ExhibitionArtistStatus.PENDING)
                    .greeting(greeting)
                    .build();

            map = exhibitionArtistMapRepository.save(map);
        } else {
            // DENIED, WITHDRAWN, REMOVED 상태의 재신청
            map.reapply(greeting);
        }

        return new ArtistJoinResponse(
                context.exhibition().getId(),
                map.getStatus()
        );
    }

    @Override
    public ExhibitionArtistStatusUpdateResponse updateArtistStatuses(
            UUID accountId,
            UUID exhibitionId,
            List<UUID> artistIds,
            ExhibitionArtistStatus targetStatus
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));
        requireCanManageArtists(actor, exhibition);

        requireAllowedTargetStatus(targetStatus);

        List<UUID> distinctArtistIds = new ArrayList<>(
                new LinkedHashSet<>(artistIds)
        );

        List<ExhibitionArtistMap> maps =
                exhibitionArtistMapRepository
                        .findAllByExhibitionIdAndArtistIdIn(
                                exhibitionId,
                                distinctArtistIds
                        );

        if (maps.size() != distinctArtistIds.size()) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_FOUND
            );
        }

        // 모든 상태 전이를 먼저 검사하여 일부만 변경되는 상황을 방지한다.
        for (ExhibitionArtistMap map : maps) {
            requireStatusTransition(map, targetStatus);
        }

        for (ExhibitionArtistMap map : maps) {
            map.updateStatus(targetStatus);
        }

        if (targetStatus == ExhibitionArtistStatus.JOINED) {
            createMissingArtistProfiles(exhibition, maps);
        } else if (targetStatus == ExhibitionArtistStatus.REMOVED) {
            cancelArtworkSubmissions(exhibitionId, distinctArtistIds);
        }

        return new ExhibitionArtistStatusUpdateResponse(maps.size());
    }

    private void requireStatusTransition(
            ExhibitionArtistMap map,
            ExhibitionArtistStatus targetStatus
    ) {
        if (!map.getStatus().canChangeTo(targetStatus)) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_STATUS_INVALID
            );
        }
    }

    private void cancelArtworkSubmissions(
            UUID exhibitionId,
            List<UUID> removedArtistIds
    ) {
        List<Artwork> artworks = artworkRepository
                .findSubmittedArtworksByExhibitionIdAndArtistIdIn(
                        exhibitionId,
                        removedArtistIds
                );

        if (artworks.isEmpty()) {
            return;
        }

        Set<UUID> coArtistIds = artworks.stream()
                .flatMap(artwork -> artwork.getArtworkArtistMaps().stream())
                .map(map -> map.getArtist().getId())
                .collect(Collectors.toSet());

        Set<UUID> joinedArtistIds = new HashSet<>(
                exhibitionArtistMapRepository.findJoinedArtistIds(
                        exhibitionId,
                        coArtistIds
                )
        );

        artworks.stream()
                .filter(artwork -> artwork.getArtworkArtistMaps().stream()
                        .noneMatch(map ->
                                joinedArtistIds.contains(map.getArtist().getId())
                        ))
                .forEach(Artwork::cancelExhibitionSubmission);
    }

    private void requireAllowedTargetStatus(
            ExhibitionArtistStatus targetStatus
    ) {
        if (targetStatus != ExhibitionArtistStatus.JOINED
                && targetStatus != ExhibitionArtistStatus.DENIED
                && targetStatus != ExhibitionArtistStatus.REMOVED) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_STATUS_INVALID
            );
        }
    }

    private void createMissingArtistProfiles(
            Exhibition exhibition,
            List<ExhibitionArtistMap> maps
    ) {
        List<UUID> artistIds = maps.stream()
                .map(map -> map.getArtist().getId())
                .toList();

        Set<UUID> existingProfileArtistIds = new HashSet<>(
                artistProfileRepository
                        .findArtistIdsByExhibitionIdAndArtistIdIn(
                                exhibition.getId(),
                                artistIds
                        )
        );

        List<ArtistProfile> profilesToCreate = maps.stream()
                .map(ExhibitionArtistMap::getArtist)
                .filter(artist -> !existingProfileArtistIds.contains(artist.getId()))
                .map(artist -> {
                    ArtistProfile profile = ArtistProfile.builder()
                            .artist(artist)
                            .exhibition(exhibition)
                            .isPublic(true)
                            .build();
                    profile.fillDefaultInfoFromArtist();
                    return profile;
                })
                .toList();

        if (!profilesToCreate.isEmpty()) {
            artistProfileRepository.saveAll(profilesToCreate);
        }
    }

    private String normalizeJoinCode(String rawCode) {
        if (rawCode == null) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.ARTIST_JOIN_CODE_INVALID
            );
        }

        String normalized = rawCode.strip().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[2-9A-HJ-KM-NP-Z]{8}")) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.ARTIST_JOIN_CODE_INVALID
            );
        }

        return normalized;
    }

    private record JoinContext(
            Artist artist,
            Exhibition exhibition,
            ExhibitionArtistMap existingMap
    ) {
    }

    private JoinContext resolveJoinContext(
            UUID accountId,
            String rawCode
    ) {
        Artist artist = artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);

        String joinCode = normalizeJoinCode(rawCode);

        Exhibition exhibition =
                exhibitionRepository.findByArtistJoinCode(joinCode)
                        .orElseThrow(() -> new ExhibitionException(
                                ExhibitionErrorCode.ARTIST_JOIN_CODE_INVALID
                        ));

        exhibition.requireAvailable();
        exhibition.requireArtistJoinCodeValid();

        ExhibitionArtistMap existingMap =
                exhibitionArtistMapRepository
                        .findByExhibitionIdAndArtistId(
                                exhibition.getId(),
                                artist.getId()
                        )
                        .orElse(null);

        if (existingMap != null
                && existingMap.getStatus().blocksReapplication()) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_ALREADY_APPLIED
            );
        }

        return new JoinContext(
                artist,
                exhibition,
                existingMap
        );
    }

}
