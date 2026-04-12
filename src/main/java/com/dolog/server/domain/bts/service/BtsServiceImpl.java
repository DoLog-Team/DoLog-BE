package com.dolog.server.domain.bts.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.bts.entity.Bts;
import com.dolog.server.domain.bts.entity.BtsArtworkMap;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dolog.server.global.util.FileService;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BtsServiceImpl implements BtsService {

    private final BtsRepository btsRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;
    private final FileService fileService;

    @Transactional
    @Override
    public BtsCreateResponse createBts(BtsCreateRequest request) throws IOException {
        // 1. 디버깅 로그: 실제로 뭐가 들어오는지 콘솔에 찍어보기
        System.out.println(">>> Request ArtworkIds: " + request.getArtworkIds());

        // 2. null 체크 방어 로직 (이걸 안 하면 findAllById에서 터짐)
        if (request.getArtworkIds() == null || request.getArtworkIds().isEmpty()) {
            throw new IllegalArgumentException("작품 ID가 전달되지 않았습니다. Postman의 Key 이름을 확인하세요 (예: artworkIds[0])");
        }

        // 3. 리스트 안에 null이 섞여 들어오는 경우 필터링
        List<UUID> safeIds = request.getArtworkIds().stream()
                .filter(Objects::nonNull)
                .toList();

        if (safeIds.isEmpty()) {
            throw new IllegalArgumentException("유효한 UUID가 없습니다.");
        }

        // 4. 이제 안전하게 조회
        List<Artwork> artworks = artworkRepository.findAllById(safeIds);

        // 2. 기준 정보 추출 (첫 번째 작품 기준)
        Artwork representativeArtwork = artworks.get(0);
        Exhibition exhibition = representativeArtwork.getExhibition();

        // 첫 번째 작품에 연결된 첫 번째 작가를 대표 작가로 설정
        Artist artist = representativeArtwork.getArtworkArtistMaps().stream()
                .findFirst()
                .map(ArtworkArtistMap::getArtist)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 3. 파일 업로드 (FileService 사용)
        String dbImageUrl = fileService.uploadFile(request.getMainImg(), "bts");

        // 4. BTS 엔티티 생성
        Bts bts = Bts.builder()
                .exhibition(exhibition)
                .artist(artist)
                .title(request.getTitle())
                .mainImg(dbImageUrl)
                .contentUrl(request.getContentUrl())
                .artworkMaps(new ArrayList<>()) // 리스트 초기화 확인
                .build();

        // 5. 모든 작품을 BTS와 매핑 (N:M 처리)
        for (Artwork artwork : artworks) {
            BtsArtworkMap map = BtsArtworkMap.builder()
                    .bts(bts)
                    .artwork(artwork)
                    .build();
            bts.getArtworkMaps().add(map);
        }

        btsRepository.save(bts);
        return BtsCreateResponse.from(bts);
    }
}
