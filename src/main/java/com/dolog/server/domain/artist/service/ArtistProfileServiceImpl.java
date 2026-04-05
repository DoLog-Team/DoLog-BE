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
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistProfileServiceImpl implements ArtistProfileService {

    private final ArtistProfileRepository profileRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ArtistRepository artistRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

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

        // 2. 파일 업로드 처리
        String rootDir = "C:/uploads";
        String subDir = "/artist-profiles/";
        String filename = UUID.randomUUID() + "_" + request.getProfileImg().getOriginalFilename();

        Path uploadPath = Paths.get(rootDir + subDir).toAbsolutePath().normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        request.getProfileImg().transferTo(filePath.toFile());

        // 3. DB 저장용 경로
        String dbImageUrl = "/uploads" + subDir + filename;




        // [1] Entity 생성 (빌더 패턴)
        ArtistProfile profile = ArtistProfile.builder()
                .artist(artist)
                .exhibition(exhibition)
                .nameKo(request.getNameKo())
                .nameEn(request.getNameEn())
                .bio(request.getBio())
                .email(request.getEmail())
                .profileImg(dbImageUrl)
                .isPublic(true)
                .build();

        // [2] 자동 저장 기능 실행 (값이 없는 경우 Artist 정보에서 긁어옴)
        // 이 단계에서 profile.nameKo, profile.email 등이 자동으로 채워짐
        profile.fillDefaultInfoFromArtist();

        // [3] DB 저장
        profileRepository.save(profile);

        // [4] 결과 반환
        return new ArtistProfileResponse(
                profile.getId(),
                profile.getNameKo(),
                profile.getNameEn(),
                profile.getBio(),
                profile.getEmail(),
                profile.getProfileImg()
        );
    }
}