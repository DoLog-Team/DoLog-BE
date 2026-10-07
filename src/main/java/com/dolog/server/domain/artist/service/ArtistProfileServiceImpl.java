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
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListItemResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileUpdateResponse;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistSnsNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.domain.artist.support.ArtistProfileImageValidator;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
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
        if (profileRepository.existsByArtistAndExhibition(artist, exhibition)) {
            throw new ArtistProfileAlreadyExistsException();
        }

        // 2. 파일 업로드 및 엔티티 저장
        profileImageValidator.validate(profileImg);
        String dbImageUrl = fileService.uploadFile(
                profileImg,
                "artist-profiles"
        );
        boolean isPublic = request.getIsPublic() == null
                || request.getIsPublic();

        ArtistProfile profile = ArtistProfile.builder()
                .artist(artist)
                .exhibition(exhibition)
                .nameKo(request.getNameKo().trim())
                .nameEn(request.getNameEn())
                .bio(TextUtils.normalizeNewlines(request.getBio()))
                .email(request.getEmail())
                .purchaseContactUrl(request.getPurchaseContactUrl())
                .profileImg(dbImageUrl)
                .isPublic(isPublic)
                .viewCount(0L)
                .build();

        profile.fillDefaultInfoFromArtist();
        ArtistProfile savedProfile = profileRepository.save(profile);

        // 3. 응답 반환
        return new ArtistProfileCreateResponse(savedProfile.getId());
    }


    // 프로필 수정
    @Transactional
    @Override
    public ArtistProfileUpdateResponse updateArtistProfile(
            UUID accountId,
            UUID profileId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException {

        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);

        // 1. 기존 프로필 조회
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);
        requireCanUpdateProfile(actor, profile);

        // 2. 이미지 처리 로직
        profileImageValidator.validate(profileImg);
        String oldImageUrl = profile.getProfileImg();
        String newImageUrl = null;

        // 빈 파일 part는 기존 이미지 삭제로 처리한다.
        if (profileImg != null && profileImg.isEmpty()) {
            profile.clearProfileImg();
            fileService.deleteFile(oldImageUrl);
        }
        // 새 파일 (교체)
        else if (profileImg != null && !profileImg.isEmpty()) {
            newImageUrl = fileService.uploadFile(
                    profileImg,
                    "artist-profiles"
            );
            fileService.deleteFile(oldImageUrl);
        }
        // 둘 다 아니면 -> 유지

        // 3. 엔티티 업데이트 (선택적 필드 업데이트 방어 코드 적용)
        profile.updateProfile(
                request.getNameKo(),
                request.getNameEn(),
                request.getBio() != null
                        ? TextUtils.normalizeNewlines(request.getBio())
                        : null,
                request.getEmail(),
                request.getPurchaseContactUrl(),
                request.getIsPublic(),
                newImageUrl
        );

        // 3. 응답 반환
        return new ArtistProfileUpdateResponse(profile.getId());
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

        if (actor.getRole() == Role.EXHIBITION_ADMIN
                && profile.getExhibition().getAccount().getId()
                .equals(actor.getId())) {
            return true;
        }

        return actor.getRole() == Role.ARTIST_ADMIN
                && profile.getArtist().getAccount() != null
                && profile.getArtist().getAccount().getId()
                .equals(actor.getId());
    }

    private void requireCanViewProfile(
            UUID accountId,
            ArtistProfile profile
    ) {
        boolean joined = exhibitionArtistMapRepository
                .existsByExhibitionIdAndArtistIdAndStatus(
                        profile.getExhibition().getId(),
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
            if (!canUpdateProfile(actor, profile)) {
                throw new ArtistProfileNotFoundException();
            }
            return;
        }

        if (profile.isPublic()) {
            return;
        }

        if (accountId == null) {
            throw new ArtistProfileAccessDeniedException();
        }

        Account actor = accountRepository.findById(accountId)
                .orElseThrow(JwtInvalidException::new);
        requireCanUpdateProfile(actor, profile);
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
            String visitorId
    ) {
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);
        requireCanViewProfile(accountId, profile);

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
        UUID exhibitionId = profile.getExhibition().getId();
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

        ArtistNeighbors neighbors = findArtistNeighbors(profile);
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
                .artworks(artworkResponses)
                .prevArtist(neighbors.previous())
                .nextArtist(neighbors.next())
                .likeCount(likeCount)
                .liked(liked)
                .viewCount(viewCount)
                .build();
    }

    private ArtistNeighbors findArtistNeighbors(ArtistProfile profile) {
        List<ExhibitionArtistItemResponse> artists = new ArrayList<>(
                exhibitionArtistMapRepository.findArtists(
                        profile.getExhibition().getId()
                )
        );

        SortType sortType = profile.getExhibition().getExhibitionDetail()
                != null
                ? profile.getExhibition().getExhibitionDetail().getSortType()
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
    public ArtistSnsListResponse getArtistSnsList(
            UUID accountId,
            UUID profileId
    ) {
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);
        requireCanViewProfile(accountId, profile);

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
