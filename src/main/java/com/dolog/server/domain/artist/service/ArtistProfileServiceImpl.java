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
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.util.FileService;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.PageRequest;

import java.io.IOException;
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
    private final FileService fileService;
    private final ArtistSnsRepository artistSnsRepository;
    private final BtsRepository btsRepository;
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
        if (actor.getRole() == Role.DOLOG_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.EXHIBITION_ADMIN
                && profile.getExhibition().getAccount().getId()
                .equals(actor.getId())) {
            return;
        }

        if (actor.getRole() == Role.ARTIST_ADMIN
                && profile.getArtist().getAccount() != null
                && profile.getArtist().getAccount().getId()
                .equals(actor.getId())) {
            return;
        }

        throw new ArtistProfileAccessDeniedException();
    }

    private void requireCanViewProfile(
            UUID accountId,
            ArtistProfile profile
    ) {
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
    @Transactional(readOnly = true)
    public ArtistProfileDetailResponse getArtistProfileDetail(UUID profileId) {
        // 1. 프로필 조회
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 2. SNS 리스트 변환
        List<ArtistProfileDetailResponse.SnsInfo> snsList = profile.getSnsList().stream()
                .map(sns -> ArtistProfileDetailResponse.SnsInfo.builder()
                        .snsId(sns.getId())
                        .platformName(sns.getPlatformName())
                        .url(sns.getUrl())
                        .build())
                .toList();

        // 3. BTS 리스트 변환
        List<ArtistProfileDetailResponse.BtsSummary> btsResponses = btsRepository
                .findAllByArtistProfileIdAndExhibitionId(profile.getId(), profile.getExhibition().getId())
                .stream()
                .map(bts -> ArtistProfileDetailResponse.BtsSummary.builder()
                        .btsId(bts.getId())
                        .title(bts.getTitle())
                        .mainImg(bts.getMainImg())
                        .build())
                .toList();

        // 4. 작품 리스트 변환
        UUID exhibitionId = profile.getExhibition().getId();

        List<ArtistProfileDetailResponse.ArtworkSummary> artworkResponses = profile.getArtworkArtistMaps().stream()
                .map(ArtworkArtistMap::getArtwork)
                .filter(artwork -> artwork.getExhibition() != null
                        && exhibitionId.equals(artwork.getExhibition().getId()))
                .map(artwork -> ArtistProfileDetailResponse.ArtworkSummary.builder()
                        .artworkId(artwork.getId())
                        .title(artwork.getTitle())
                        .image(artwork.getMainImg())
                        .build())
                .toList();

        // 5. prev / next 계산 (DB에서 직접 조회)
        String currentNameKo = profile.getNameKo();

        List<ArtistProfile> prevList = profileRepository.findPrevProfile(exhibitionId, currentNameKo, PageRequest.of(0, 1));
        List<ArtistProfile> nextList = profileRepository.findNextProfile(exhibitionId, currentNameKo, PageRequest.of(0, 1));

        ArtistProfile prev = prevList.isEmpty() ? null : prevList.get(0);
        ArtistProfile next = nextList.isEmpty() ? null : nextList.get(0);


        // 6. 최종 DTO 조립
        return ArtistProfileDetailResponse.builder()
                .profileId(profile.getId())
                .artistId(profile.getArtist().getId())
                .nameKo(profile.getNameKo())
                .nameEn(profile.getNameEn())
                .profileImage(profile.getProfileImg())
                .isPublic(profile.isPublic())
                .bio(profile.getBio())
                .contact(ArtistProfileDetailResponse.ContactInfo.builder()
                        .email(profile.getEmail())
                        .snsList(snsList)
                        .build())
                .behindTheScenes(btsResponses)
                .artworks(artworkResponses)
                .prevArtist(prev != null
                        ? ArtistProfileDetailResponse.NeighborArtist.builder()
                        .id(prev.getId())
                        .name(prev.getNameKo())
                        .build()
                        : null)

                .nextArtist(next != null
                        ? ArtistProfileDetailResponse.NeighborArtist.builder()
                        .id(next.getId())
                        .name(next.getNameKo())
                        .build()
                        : null)
                .build();
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
