package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.ArtistBadRequestException;
import com.dolog.server.domain.artist.exception.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.ArtistUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
}