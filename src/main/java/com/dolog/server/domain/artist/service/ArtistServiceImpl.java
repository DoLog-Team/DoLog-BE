package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;

    // 작가 생성
    @Override
    public ArtistResponse createArtist(ArtistCreateRequest request) {

        if (request.getNameKo() == null || request.getNameKo().isBlank()) {
            throw new ArtistBadRequestException();
        }

        Artist artist = Artist.builder()
                .nameKo(request.getNameKo())
                .nameEn(request.getNameEn())
                .phone(request.getPhone())
                .build();

        artistRepository.save(artist);

        return ArtistResponse.from(artist);
    }

    // 업데이트
    @Override
    public ArtistResponse updateArtist(UUID artistId, ArtistUpdateRequest request) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        artist.updateArtistInfo(
                request.getNameKo(),
                request.getNameEn(),
                request.getPhone()
        );

        return ArtistResponse.from(artist);
    }

    // 삭제
    @Override
    public ArtistResponse deleteArtist(UUID artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        ArtistResponse response = ArtistResponse.from(artist);

        artistRepository.delete(artist);

        return response;
    }


    // 작가 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> getArtists() {

        return artistRepository.findAll()
                .stream()
                .map(ArtistResponse::from)
                .collect(Collectors.toList());
    }

    // 작가 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse getArtist(UUID artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        return ArtistResponse.from(artist);
    }
}