package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.ArtistSns;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAccessDeniedException;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistNotRegisteredInExhibitionException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAlreadyExistsException;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListItemResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsUpdateResponse;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistSnsNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.domain.artist.support.ArtistProfileImageValidator;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.entity.enums.SortType;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistItemResponse;
import com.dolog.server.domain.like.repository.ArtistProfileLikeRepository;
import com.dolog.server.global.util.FileService;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistProfileServiceImpl implements ArtistProfileService {

    private final ArtistProfileRepository profileRepository;
    private final AccountRepository accountRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ArtistRepository artistRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtworkRepository artworkRepository;
    private final BtsRepository btsRepository;
    private final ArtistProfileLikeRepository artistProfileLikeRepository;
    private final FileService fileService;
    private final ArtistSnsRepository artistSnsRepository;
    private final ArtistProfileImageValidator profileImageValidator;

    // 프로필 생성
    @Transactional
    @Override
    public ArtistProfileCreateResponse createArtistProfile(
            ArtistProfileCreateRequest request,
            MultipartFile profileImg
    ) throws IOException {

        UUID exhibitionId = request.getExhibitionId();
        UUID artistId = request.getArtistId();

        // 1. 전시 및 작가 존재 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));

        var artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        // [선 등록 여부 검증] 해당 전시에 등록된 사람인지 확인
        if (!exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        exhibitionId,
                        artistId,
                        ExhibitionArtistStatus.JOINED
                )) {
            throw new ArtistNotRegisteredInExhibitionException();
        }

        // 중복 체크 (이미 프로필이 있는지)
        if (profileRepository.existsByArtistId(artistId)) {
            throw new ArtistProfileAlreadyExistsException();
        }

        // 2. 파일 업로드 및 엔티티 저장
        profileImageValidator.validate(profileImg);
        String dbImageUrl = fileService.uploadFile(
                profileImg,
                "artist-profiles"
        );
        ArtistProfile profile = ArtistProfile.builder()
                .artist(artist)
                .nameKo(request.getNameKo().trim())
                .nameEn(request.getNameEn())
                .bio(TextUtils.normalizeNewlines(request.getBio()))
                .email(request.getEmail())
                .purchaseContactUrl(request.getPurchaseContactUrl())
                .profileImg(dbImageUrl)
                .viewCount(0L)
                .build();

        profile.fillDefaultInfoFromArtist();
        ArtistProfile savedProfile = profileRepository.save(profile);

        // 3. 응답 반환
        return new ArtistProfileCreateResponse(savedProfile.getId());
    }


    private void requireCanUpdateProfile(
            Account actor,
            ArtistProfile profile
    ) {
        if (canUpdateProfile(actor, profile)) {
            return;
        }

        throw new ArtistProfileAccessDeniedException();
    }

    private boolean canUpdateProfile(
            Account actor,
            ArtistProfile profile
    ) {
        if (actor.getRole() == Role.DOLOG_ADMIN) {
            return true;
        }

        return actor.getRole() == Role.ARTIST_ADMIN
                && profile.getArtist().getAccount() != null
                && profile.getArtist().getAccount().getId()
                .equals(actor.getId());
    }

    private void requireCanViewProfile(
            UUID accountId,
            ArtistProfile profile,
            Exhibition exhibition
    ) {
        boolean joined = exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        exhibition.getId(),
                        profile.getArtist().getId(),
                        ExhibitionArtistStatus.JOINED
                );

        // 공개 전시에서 빠진 작가의 프로필 존재 여부는 일반 요청에 노출하지 않는다.
        if (!joined) {
            if (accountId == null) {
                throw new ArtistProfileNotFoundException();
            }

            Account actor = accountRepository.findById(accountId)
                    .orElseThrow(JwtInvalidException::new);
            if (!canUpdateProfile(actor, profile)
                    && !canManageExhibition(actor, exhibition)) {
                throw new ArtistProfileNotFoundException();
            }
            return;
        }

        // 프로필에는 별도 공개/비공개 상태가 없으므로,
        // 전시에 참여 중인 작가의 프로필은 누구나 조회할 수 있다.
    }

    private boolean canManageExhibition(
            Account actor,
            Exhibition exhibition
    ) {
        return actor.getRole() == Role.EXHIBITION_ADMIN
                && exhibition.getAccount().getId().equals(actor.getId());
    }

    // 관리자용 프로필 목록 조회
    @Override
    @Transactional(readOnly = true)
    public ArtistProfileListResponse getArtistProfileList(
            UUID accountId,
            UUID exhibitionId
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));

        requireCanManageProfiles(actor, exhibition);

        List<ArtistProfileListItemResponse> profiles =
                profileRepository.findListItemsByExhibitionId(exhibitionId)
                        .stream()
                        .map(ArtistProfileListItemResponse::from)
                        .toList();

        return ArtistProfileListResponse.builder()
                .profiles(profiles)
                .build();
    }

    private void requireCanManageProfiles(
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

        throw new ArtistProfileAccessDeniedException();
    }

    // 프로필 상세 조회
    @Override
    @Transactional
    public ArtistProfileDetailResponse getArtistProfileDetail(
            UUID accountId,
            UUID profileId,
            UUID exhibitionId,
            String visitorId
    ) {
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(
                        ExhibitionErrorCode.EXHIBITION_NOT_FOUND
                ));
        requireCanViewProfile(accountId, profile, exhibition);

        int updatedRows = profileRepository.incrementViewCount(profileId);
        if (updatedRows != 1) {
            throw new ArtistProfileNotFoundException();
        }
        long viewCount = profileRepository.findViewCountById(profileId);

        List<ArtistProfileDetailResponse.SnsInfo> snsList =
                artistSnsRepository.findByArtistProfileId(profileId).stream()
                .map(sns -> ArtistProfileDetailResponse.SnsInfo.builder()
                        .snsId(sns.getId())
                        .platformName(sns.getPlatformName())
                        .url(sns.getUrl())
                        .build())
                .toList();

        UUID artistId = profile.getArtist().getId();
        List<ArtistProfileDetailResponse.ArtworkSummary> artworkResponses =
                artworkRepository.findVisibleInExhibitionByArtistId(
                                exhibitionId,
                                artistId
                        )
                .stream()
                .map(artwork -> ArtistProfileDetailResponse.ArtworkSummary.builder()
                        .artworkId(artwork.getId())
                        .title(artwork.getTitle())
                        .mainImg(artwork.getMainImg())
                        .build())
                .toList();

        List<ArtistProfileDetailResponse.BtsSummary> btsResponses =
                btsRepository.findAllByArtistProfileIdAndExhibitionId(
                                profileId,
                                exhibitionId
                        )
                        .stream()
                        .map(bts -> ArtistProfileDetailResponse.BtsSummary
                                .builder()
                                .btsId(bts.getId())
                                .title(bts.getTitle())
                                .mainImg(bts.getMainImg())
                                .build())
                        .toList();

        ArtistNeighbors neighbors = findArtistNeighbors(
                profile,
                exhibition
        );
        int likeCount = Math.toIntExact(
                artistProfileLikeRepository.countByArtistProfileId(profileId)
        );
        boolean liked = visitorId != null
                && artistProfileLikeRepository
                .existsByArtistProfileIdAndVisitorId(
                        profileId,
                        visitorId
                );

        return ArtistProfileDetailResponse.builder()
                .profileId(profile.getId())
                .artistId(artistId)
                .exhibitionId(exhibitionId)
                .nameKo(profile.getNameKo())
                .nameEn(profile.getNameEn())
                .bio(profile.getBio())
                .profileImg(profile.getProfileImg())
                .email(profile.getEmail())
                .snsList(snsList)
                .purchaseContactUrl(profile.getPurchaseContactUrl())
                .behindTheScenes(btsResponses)
                .artworks(artworkResponses)
                .prevArtist(neighbors.previous())
                .nextArtist(neighbors.next())
                .likeCount(likeCount)
                .liked(liked)
                .viewCount(viewCount)
                .build();
    }

    private ArtistNeighbors findArtistNeighbors(
            ArtistProfile profile,
            Exhibition exhibition
    ) {
        List<ExhibitionArtistItemResponse> artists = new ArrayList<>(
                exhibitionArtistMapRepository.findArtists(
                        exhibition.getId()
                )
        );

        SortType sortType = exhibition.getExhibitionDetail()
                != null
                ? exhibition.getExhibitionDetail().getSortType()
                : SortType.ABC;

        if (sortType == SortType.RANDOM) {
            Collections.shuffle(artists);
        } else {
            artists.sort(
                    Comparator.comparing(
                                    ExhibitionArtistItemResponse::getNameKo,
                                    Comparator.nullsLast(
                                            Comparator.naturalOrder()
                                    )
                            )
                            .thenComparing(
                                    ExhibitionArtistItemResponse::getProfileId,
                                    Comparator.nullsLast(
                                            Comparator.naturalOrder()
                                    )
                            )
            );
        }

        int currentIndex = -1;
        for (int index = 0; index < artists.size(); index++) {
            if (profile.getId().equals(artists.get(index).getProfileId())) {
                currentIndex = index;
                break;
            }
        }

        if (currentIndex < 0) {
            return new ArtistNeighbors(null, null);
        }

        ArtistProfileDetailResponse.NeighborArtist previous =
                currentIndex > 0
                        ? toNeighbor(artists.get(currentIndex - 1))
                        : null;
        ArtistProfileDetailResponse.NeighborArtist next =
                currentIndex < artists.size() - 1
                        ? toNeighbor(artists.get(currentIndex + 1))
                        : null;
        return new ArtistNeighbors(previous, next);
    }

    private ArtistProfileDetailResponse.NeighborArtist toNeighbor(
            ExhibitionArtistItemResponse artist
    ) {
        return ArtistProfileDetailResponse.NeighborArtist.builder()
                .profileId(artist.getProfileId())
                .nameKo(artist.getNameKo())
                .profileImg(artist.getProfileImg())
                .build();
    }

    private record ArtistNeighbors(
            ArtistProfileDetailResponse.NeighborArtist previous,
            ArtistProfileDetailResponse.NeighborArtist next
    ) {
    }


//----------------[ SNS ] ----------------------

    // SNS 추가
    @Transactional
    @Override
    public ArtistSnsCreateResponse addArtistSns(
            UUID accountId,
            UUID profileId,
            ArtistSnsRequest request
    ) {

        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        // 1. 프로필 존재 확인
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);
        requireCanUpdateProfile(actor, profile);

        // 2. SNS 엔티티 생성 및 저장
        ArtistSns sns = ArtistSns.builder()
                .artistProfile(profile)
                .platformName(request.getPlatformName().trim())
                .url(request.getUrl().trim())
                .build();

        ArtistSns savedSns = artistSnsRepository.save(sns);

        // 3. 응답 반환
        return new ArtistSnsCreateResponse(savedSns.getId());
    }


    // SNS 삭제
    @Transactional
    @Override
    public void deleteArtistSns(UUID accountId, UUID snsId) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);
        ArtistSns sns = artistSnsRepository.findById(snsId)
                .orElseThrow(ArtistSnsNotFoundException::new);

        requireCanUpdateProfile(actor, sns.getArtistProfile());
        artistSnsRepository.delete(sns);
    }

    // SNS 목록 조회
    @Transactional(readOnly = true)
    @Override
    public ArtistSnsListResponse getArtistSnsList(UUID profileId) {
        profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        List<ArtistSnsResponse> snsList =
                artistSnsRepository.findByArtistProfileId(profileId)
                .stream()
                .map(ArtistSnsResponse::from)
                .toList();

        return new ArtistSnsListResponse(snsList);
    }

    // SNS 수정
    @Transactional
    @Override
    public ArtistSnsUpdateResponse updateArtistSns(
            UUID accountId,
            UUID snsId,
            ArtistSnsUpdateRequest request
    ) {
        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);
        ArtistSns sns = artistSnsRepository.findById(snsId)
                .orElseThrow(ArtistSnsNotFoundException::new);

        requireCanUpdateProfile(actor, sns.getArtistProfile());
        sns.update(request.getPlatformName(), request.getUrl());

        return new ArtistSnsUpdateResponse(sns.getId());
    }
}
