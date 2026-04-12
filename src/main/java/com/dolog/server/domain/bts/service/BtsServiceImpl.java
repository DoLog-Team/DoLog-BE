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
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsCreateResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;
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
import java.util.stream.Collectors;

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

        Bts savedBts = btsRepository.save(bts);
        return BtsCreateResponse.of(savedBts);
    }

    @Override
    @Transactional
    public BtsCreateResponse updateBts(UUID btsId, BtsUpdateRequest request) {
        // 1. BTS 조회
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new RuntimeException("BTS content not found"));

        // 2. 작가 정보 업데이트 (전달된 ID가 있으면 조회, 없으면 기존 유지)
        Artist artist = bts.getArtist();
        if (request.getArtistId() != null) {
            artist = artistRepository.findById(request.getArtistId())
                    .orElseThrow(() -> new RuntimeException("Artist not found"));
        }

        // 3. BTS 기본 정보 업데이트
        bts.updateBtsInfo(request.getTitle(), request.getContentUrl(), request.getMainImg(), artist);

        // 4. 연결된 작품 목록(ArtworkMaps) 업데이트
        if (request.getArtworkIds() != null) {
            // 기존 매핑 제거 (orphanRemoval = true 설정 덕분에 리스트 비우면 DB에서도 삭제됨)
            bts.getArtworkMaps().clear();

            // 새로운 작품들 조회 및 매핑 추가
            List<Artwork> artworks = artworkRepository.findAllById(request.getArtworkIds());
            for (Artwork artwork : artworks) {
                BtsArtworkMap map = BtsArtworkMap.builder()
                        .bts(bts)
                        .artwork(artwork)
                        .build();
                bts.getArtworkMaps().add(map);
            }
        }

        // 5. 변경된 전체 정보 응답
        return BtsCreateResponse.of(bts);
    }

    @Override
    @Transactional
    public void deleteBts(UUID btsId) {
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new RuntimeException("BTS content not found"));

        // cascade = ALL 설정에 의해 bts_artwork_map도 같이 삭제됨
        btsRepository.delete(bts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BtsListResponse> getExhibitionBtsList(UUID exhibitionId) {
        // 1. 해당 전시회의 모든 BTS 조회
        List<Bts> btsList = btsRepository.findAllByExhibitionId(exhibitionId);

        // 2. DTO 리스트로 변환
        return btsList.stream()
                .map(BtsListResponse::from)
                .collect(Collectors.toList());
    }
}
