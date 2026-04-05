package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileAlreadyExistsException;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.exhibition.entity.Exhibition;
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

    @Transactional
    @Override
    public ArtistProfileResponse createArtistProfile(String exhibitionIdStr, ArtistProfileCreateRequest request) throws IOException {

        UUID exhibitionId = UUID.fromString(exhibitionIdStr);
        UUID artistId = UUID.fromString(request.getArtistId());

        // 1. 존재 여부 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(ArtistProfileNotFoundException::new);

        var artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        // 중복 체크
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

        // 4. Entity 생성 및 저장
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

        profileRepository.save(profile);

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