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
public class ArtistService {

    private final ArtistRepository artistRepository;

    //작가 생성
    public ArtistResponse createArtist(ArtistCreateRequest request) {

        if (request.getNameKo() == null || request.getNameKo().isBlank()) {
            throw new ArtistBadRequestException();
        }

        // 1. Account당 Artist 하나 제한 -> V2 에 적용
//        if (artistRepository.existsByAccount(account)) {
//            throw new IllegalStateException("이미 Artist가 존재합니다.");
//        }

        // 2. Artist 생성
        Artist artist = Artist.builder()
//                .account(account)
                .nameKo(request.getNameKo())
                .nameEn(request.getNameEn())
                .phone(request.getPhone())
                .build();

        artistRepository.save(artist);

        // 3. Response 반환
        return ArtistResponse.from(artist);
    }

    // 업데이트
    @Transactional
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
    @Transactional
    public ArtistResponse deleteArtist(UUID artistId) {

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        // 삭제 전에 DTO로 변환
        ArtistResponse response = ArtistResponse.from(artist);

        artistRepository.delete(artist);

        return response;
    }
}
