package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.entity.ArtistSns;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistNotRegisteredInExhibitionException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAlreadyExistsException;
import com.dolog.server.domain.artist.repository.ArtistSnsRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.basic.ExhibitionListItemResponse;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistProfileServiceImpl implements ArtistProfileService {

    private final ArtistProfileRepository profileRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final ArtistRepository artistRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final FileService fileService;
    private final ArtistSnsRepository artistSnsRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final BtsRepository btsRepository;

    // 프로필 생성
    @Transactional
    @Override
    public ArtistProfileResponse createArtistProfile(String exhibitionIdStr, ArtistProfileCreateRequest request) throws IOException {

        UUID exhibitionId = UUID.fromString(exhibitionIdStr);
        UUID artistId = UUID.fromString(request.getArtistId());

        // 1. 전시 및 작가 존재 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        var artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        // [선 등록 여부 검증] 해당 전시에 등록된 사람인지 확인
        if (!exhibitionArtistMapRepository.existsByExhibitionIdAndArtistId(exhibitionId, artistId)) {
            throw new ArtistNotRegisteredInExhibitionException();
        }

        // 중복 체크 (이미 프로필이 있는지)
        if (profileRepository.existsByArtistAndExhibition(artist, exhibition)) {
            throw new ArtistProfileAlreadyExistsException();
        }

        // 2. 파일 업로드 및 엔티티 저장
        String dbImageUrl = fileService.uploadFile(request.getProfileImg(), "artist-profiles");
        ArtistProfile profile = ArtistProfile.builder()
                .artist(artist).exhibition(exhibition)
                .nameKo(request.getNameKo()).nameEn(request.getNameEn())
                .bio(request.getBio() != null ? request.getBio().replace("\\n", "\n") : null).email(request.getEmail())
                .profileImg(dbImageUrl).isPublic(true).build();

        profile.fillDefaultInfoFromArtist();
        profileRepository.save(profile);

        // 3. 응답 반환
        return convertToResponse(profile);
    }


    // 프로필 수정
    @Transactional
    @Override
    public ArtistProfileResponse updateArtistProfile(String profileIdStr, ArtistProfileCreateRequest request) throws IOException {

        // 1. 기존 프로필 조회
        UUID profileId = UUID.fromString(profileIdStr);
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 2. 이미지 처리 로직
        String newImageUrl = profile.getProfileImg(); // 기본값은 기존 이미지 유지

        // 이미지 삭제 플래그가 true
        if (Boolean.TRUE.equals(request.getIsDeleteImg())) {
            if (profile.getProfileImg() != null) {
                fileService.deleteFile(profile.getProfileImg());
            }
            newImageUrl = null;
        }
        // 새 파일 (교체)
        else if (request.getProfileImg() != null && !request.getProfileImg().isEmpty()) {
            if (profile.getProfileImg() != null) {
                fileService.deleteFile(profile.getProfileImg());
            }
            newImageUrl = fileService.uploadFile(request.getProfileImg(), "artist-profiles");
        }
        // 둘 다 아니면 -> 유지

        // 3. 엔티티 업데이트
        profile.updateProfile(
                request.getNameKo(),
                request.getNameEn(),
                request.getBio() != null ? request.getBio().replace("\\n", "\n") : null,
                request.getEmail(),
                newImageUrl
        );

        // 3. 응답 반환
        return convertToResponse(profile);
    }

    // 프로필 목록 조회 (DB 조회용)
    @Override
    @Transactional(readOnly = true)
    public List<ArtistProfileResponse> getArtistProfileList(UUID exhibitionId) {
        List<ArtistProfile> profiles;

        if (exhibitionId != null) {
            profiles = profileRepository.findAllByExhibitionId(exhibitionId);
        } else {
            profiles = profileRepository.findAll();
        }

        return profiles.stream()
                .map(this::convertToResponse)
                .toList();
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
                .findAllByArtistProfileIdAndExhibitionId(profile.getArtist().getId(), profile.getExhibition().getId())
                .stream()
                .map(bts -> ArtistProfileDetailResponse.BtsSummary.builder()
                        .btsId(bts.getId())
                        .title(bts.getTitle())
                        .mainImg(bts.getMainImg())
                        .build())
                .toList();

        // 4. 작품 리스트 변환
        List<ArtistProfileDetailResponse.ArtworkSummary> artworkResponses = profile.getArtworkArtistMaps().stream()
                .map(ArtworkArtistMap::getArtwork)
                .map(artwork -> ArtistProfileDetailResponse.ArtworkSummary.builder()
                        .artworkId(artwork.getId())
                        .title(artwork.getTitle())
                        .image(artwork.getMainImg())
                        .build())
                .toList();

        // 5. prev / next 계산
        List<ArtistProfile> profiles =
                profileRepository.findAllByExhibitionId(profile.getExhibition().getId());

        // 가나다 정렬
        profiles.sort(Comparator.comparing(
                ArtistProfile::getNameKo,
                Collator.getInstance(Locale.KOREAN)
        ));

        int currentIndex = -1;

        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(profileId)) {
                currentIndex = i;
                break;
            }
        }

        ArtistProfile prev = currentIndex > 0 ? profiles.get(currentIndex - 1) : null;
        ArtistProfile next = currentIndex < profiles.size() - 1 ? profiles.get(currentIndex + 1) : null;


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
    public ArtistSnsResponse addArtistSns(String profileIdStr, ArtistSnsRequest request) {
        UUID profileId = UUID.fromString(profileIdStr);

        // 1. 프로필 존재 확인
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 2. SNS 엔티티 생성 및 저장
        ArtistSns sns = ArtistSns.builder()
                .artistProfile(profile)
                .platformName(request.getPlatformName())
                .url(request.getUrl())
                .build();

        ArtistSns savedSns = artistSnsRepository.save(sns);

        // 3. 응답 반환
        return ArtistSnsResponse.from(savedSns);
    }


    // SNS 삭제
    @Transactional
    @Override
    public List<ArtistSnsResponse> deleteArtistSns(UUID snsId) {
        // 1. 삭제할 SNS 조회 (프로필 ID를 알아내기 위해 먼저 조회)
        ArtistSns sns = artistSnsRepository.findById(snsId)
                .orElseThrow(() -> new RuntimeException("해당 SNS 기록을 찾을 수 없습니다."));

        UUID profileId = sns.getArtistProfile().getId();

        // 2. 삭제 수행
        artistSnsRepository.delete(sns);

        // 3. 삭제 후 해당 프로필의 "남은 SNS 목록"을 다시 조회해서 반환
        return artistSnsRepository.findByArtistProfileId(profileId)
                .stream()
                .map(ArtistSnsResponse::from)
                .toList();
    }

    // SNS 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<ArtistSnsResponse> getArtistSnsList(String profileIdStr) {
        UUID profileId = UUID.fromString(profileIdStr);

        // DB에서 해당 프로필 ID를 외래키로 가진 SNS들을 다 긁어옵니다.
        return artistSnsRepository.findByArtistProfileId(profileId)
                .stream()
                .map(ArtistSnsResponse::from)
                .toList();
    }




    // 응답
    private ArtistProfileResponse convertToResponse(ArtistProfile profile) {
        // 1. 프로필에 연결된 전시 본체(Exhibition)를 가져옵니다.
        Exhibition exhibition = profile.getExhibition();

        // 2. 전시의 상세 정보(ExhibitionDetail)를 찾습니다.
        var exhibitionDetail = exhibitionDetailRepository.findByExhibition(exhibition)
                .orElse(null);

        // 엔티티 내부 리스트 사용
        List<ArtistSnsResponse> snsList = profile.getSnsList().stream()
                .map(ArtistSnsResponse::from)
                .toList();

        // 3. ArtistProfileResponse를 빌더로 만듭니다.
        return ArtistProfileResponse.builder()
                .profileId(profile.getId())
                .nameKo(profile.getNameKo())
                .nameEn(profile.getNameEn())
                .bio(profile.getBio())
                .email(profile.getEmail())
                .profileImg(profile.getProfileImg())
                .snsList(snsList)
                .exhibition(ExhibitionListItemResponse.of(
                        exhibition,
                        exhibitionDetail,
                        LocalDate.now()
                ))
                .build();
    }

}