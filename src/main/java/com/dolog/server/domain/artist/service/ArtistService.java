package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.ArtistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    public ArtistResponse createArtist(ArtistCreateRequest request) {

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
}
