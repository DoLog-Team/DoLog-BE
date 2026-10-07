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
import com.dolog.server.domain.artwork.support.ArtworkSubmissionCanceller;
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
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageItemResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistStatusUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinCodeValidateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinResponse;
import com.dolog.server.global.exception.jwt.JwtInvalidException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.*;
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
    private final ArtworkSubmissionCanceller artworkSubmissionCanceller;

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

    // 권한 조회가 락보다 앞서므로 호출 트랜잭션은 READ_COMMITTED를 사용한다.
    // 그래야 락 대기 후 후속 조회가 앞선 요청의 커밋 결과를 볼 수 있다.
    private Exhibition findAuthorizedExhibitionForUpdate(
            UUID accountId,
            UUID exhibitionId
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));

        requireCanManageArtists(actor, exhibition);

        // 권한을 확인한 요청만 같은 전시의 작가 변경 작업을 직렬화한다.
        return exhibitionRepository.findByIdForUpdate(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeArtistFromExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    ) {
        findAuthorizedExhibitionForUpdate(accountId, exhibitionId);

        ExhibitionArtistMap map = exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(exhibitionId, artistId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_FOUND
                ));

        requireStatusTransition(map, ExhibitionArtistStatus.REMOVED);
        map.updateStatus(ExhibitionArtistStatus.REMOVED);
        cancelArtworkSubmissions(exhibitionId, List.of(artistId));
    }

    // 작가 본인의 전시 나가기 (참여 이력과 프로필은 유지한다)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leaveExhibition(UUID accountId, UUID exhibitionId) {
        Artist artist = artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);

        UUID artistId = artist.getId();

        // 참여 여부를 먼저 확인하여 무관한 작가가 전시 행 락을 잡지 못하게 한다.
        if (!exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        exhibitionId,
                        artistId,
                        ExhibitionArtistStatus.JOINED
                )) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_JOINED
            );
        }

        // 같은 전시의 제외·나가기 요청을 직렬화하여 공동 작품이 남는 경쟁 상태를 막는다.
        exhibitionRepository.findByIdForUpdate(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_JOINED
                ));

        // 락을 기다리는 동안 상태가 바뀌었을 수 있으므로 참여 관계를 다시 조회한다.
        ExhibitionArtistMap map = exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(exhibitionId, artistId)
                .filter(joinedMap ->
                        joinedMap.getStatus() == ExhibitionArtistStatus.JOINED
                )
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_JOINED
                ));

        map.updateStatus(ExhibitionArtistStatus.WITHDRAWN);
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExhibitionArtistStatusUpdateResponse updateArtistStatuses(
            UUID accountId,
            UUID exhibitionId,
            List<UUID> artistIds,
            ExhibitionArtistStatus targetStatus
    ) {
        Exhibition exhibition = findAuthorizedExhibitionForUpdate(
                accountId,
                exhibitionId
        );

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

    @Override
    @Transactional(readOnly = true)
    // 관리자용 작가 목록을 조회하기 전에 요청값과 권한을 검사하고, 검색 조건을 정리한 뒤 페이지 단위로 조회
    public ExhibitionArtistManageListResponse getArtistsForManagement(
            UUID accountId,
            UUID exhibitionId,
            ExhibitionArtistStatus status,
            String search,
            int page,
            int size
    ) {
        if ((status != ExhibitionArtistStatus.PENDING
                && status != ExhibitionArtistStatus.JOINED)
                || page < 0
                || size < 1
                || size > 100) {
            throw new ExhibitionException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_QUERY_INVALID
            );
        }

        // 요청을 보낸 사용자 정보 조회
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        // 관리 대상 전시가 실제로 존재하는지 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));

        // DOLOG_ADMIN이거나 해당 전시 ADMIN인지 확인
        requireCanManageArtists(actor, exhibition);

        String normalizedSearch =
                search == null || search.isBlank()
                        ? null
                        : escapeLikePattern(search.trim());

        Page<ExhibitionArtistMap> result =
                exhibitionArtistMapRepository.findArtistsForManagement(
                        exhibitionId,
                        status,
                        normalizedSearch,
                        PageRequest.of(page, size)
                );

        Map<UUID, Integer> artworkCountByArtistId = new HashMap<>();

        if (status == ExhibitionArtistStatus.JOINED
                && !result.getContent().isEmpty()) {

            List<UUID> artistIds = result.getContent().stream()
                    .map(map -> map.getArtist().getId())
                    .toList();

            exhibitionArtistMapRepository
                    .countArtworksByArtistIds(exhibitionId, artistIds)
                    .forEach(count -> artworkCountByArtistId.put(
                            count.getArtistId(),
                            Math.toIntExact(count.getArtworkCount())
                    ));
        }

        List<ExhibitionArtistManageItemResponse> artists =
                result.getContent().stream()
                        .map(map -> {
                            Artist artist = map.getArtist();

                            Integer artworkCount =
                                    status == ExhibitionArtistStatus.JOINED
                                            ? artworkCountByArtistId.getOrDefault(
                                                    artist.getId(),
                                                    0
                                            )
                                            : null;

                            return new ExhibitionArtistManageItemResponse(
                                    artist.getId(),
                                    artist.getNameKo(),
                                    artist.getAccount() == null
                                            ? null
                                            : artist.getAccount().getEmail(),
                                    map.getGreeting(),
                                    artworkCount
                            );
                        })
                        .toList();

        return new ExhibitionArtistManageListResponse(
                artists,
                Math.toIntExact(result.getTotalElements()),
                result.getTotalPages()
        );
    }

    private String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
                .forEach(artworkSubmissionCanceller::cancel);
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
