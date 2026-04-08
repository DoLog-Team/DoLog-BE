package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.exception.artistProfileError.ArtistNotRegisteredInExhibitionException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAlreadyExistsException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileErrorCode;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionListItemResponse;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
                .bio(request.getBio()).email(request.getEmail())
                .profileImg(dbImageUrl).isPublic(true).build();

        profile.fillDefaultInfoFromArtist();
        profileRepository.save(profile);

        // 3. 응답 반환
        return convertToResponse(profile);
    }

    @Transactional
    @Override
    public ArtistProfileResponse updateArtistProfile(String profileIdStr, ArtistProfileCreateRequest request) throws IOException {

        // 1. 기존 프로필 조회
        UUID profileId = UUID.fromString(profileIdStr);
        ArtistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 2. 이미지 및 필드 업데이트
        String newImageUrl = null;
        if (request.getProfileImg() != null && !request.getProfileImg().isEmpty()) {
            fileService.deleteFile(profile.getProfileImg()); // 기존 파일 삭제
            newImageUrl = fileService.uploadFile(request.getProfileImg(), "artist-profiles"); // 새 파일 저장
        }

        // 2. 엔티티 업데이트 (이미지 경로까지 한 번에 전달!)
        // 이미지가 없으면 null이 넘어가고, 엔티티 내부의 if문에서 null 체크를 하니까 안전합니다.
        profile.updateProfile(
                request.getNameKo(),
                request.getNameEn(),
                request.getBio(),
                request.getEmail(),
                newImageUrl
        );

        // 3. 응답 반환
        return convertToResponse(profile);
    }


    // 서비스 클래스 내부 맨 아래
    private ArtistProfileResponse convertToResponse(ArtistProfile profile) {
        // 1. 프로필에 연결된 전시 본체(Exhibition)를 가져옵니다.
        Exhibition exhibition = profile.getExhibition();

        // 2. 전시의 상세 정보(ExhibitionDetail)를 찾습니다.
        var exhibitionDetail = exhibitionDetailRepository.findByExhibition(exhibition)
                .orElse(null);

        // 3. ArtistProfileResponse를 빌더로 만듭니다.
        return ArtistProfileResponse.builder()
                .profileId(profile.getId())
                .nameKo(profile.getNameKo())
                .nameEn(profile.getNameEn())
                .bio(profile.getBio())
                .email(profile.getEmail())
                .profileImg(profile.getProfileImg())
                // ★ 핵심 변경: detail이 null이어도 exhibition(기본정보)을 넘겨서 객체를 생성하게 함!
                .exhibition(ExhibitionListItemResponse.of(exhibition, exhibitionDetail))
                .build();
    }

}