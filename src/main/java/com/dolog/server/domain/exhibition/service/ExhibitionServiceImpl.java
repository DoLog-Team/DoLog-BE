package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.exception.ExhibitionArtistException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionServiceImpl implements ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final ExhibitionMapRepository exhibitionMapRepository;
    private final ArtistRepository artistRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Override
    @Transactional(readOnly = true)
    public ExhibitionMainResponse getMainExhibitions() {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findTop3PublicExhibitions(PageRequest.of(0, 3));
        List<ExhibitionListItemResponse> items = details.stream()
                .map(ExhibitionListItemResponse::from)
                .collect(Collectors.toList());
        return ExhibitionMainResponse.builder().mainExhibitions(items).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExhibitionListItemResponse> getExhibitions(Boolean isPublic, String univName, String search) {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findExhibitions(isPublic, univName, search);
        return details.stream()
                .map(ExhibitionListItemResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request) {
        Exhibition exhibition = Exhibition.builder()
                .account(null) //TODO: JWT -> v2 에서 연동함
                .univName(request.getUnivName())
                .deptName(request.getDeptName())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        Exhibition saved = exhibitionRepository.save(exhibition);

        return ExhibitionCreateResponse.builder()
                .id(saved.getId().toString())
                .message("전시회가 성공적으로 등록되었습니다.")
                .build();
    }

    @Override
    public ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        exhibition.updateBasicInfo(request.getUnivName(), request.getDeptName(), request.getIsPublic());

        return ExhibitionMessageResponse.builder()
                .message("기본 정보가 성공적으로 수정되었습니다.")
                .build();
    }

    @Override
    public ExhibitionMessageResponse deleteExhibition(UUID exhibitionId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        exhibitionRepository.delete(exhibition);

        return ExhibitionMessageResponse.builder()
                .message("전시회가 성공적으로 삭제되었습니다.")
                .build();
    }

    // 전시 작가 추가
    @Override
    public ExhibitionArtistAddResponse addArtistToExhibition(UUID exhibitionId, UUID artistId) {

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(ArtistNotFoundException::new);

        // ✅ 중복 체크
        if (exhibitionArtistMapRepository.existsByExhibitionIdAndArtistId(exhibitionId, artistId)) {
            throw new ExhibitionArtistException(
                    ExhibitionErrorCode.EXHIBITION_ARTIST_ALREADY_EXISTS
            );
        }

        // ✅ 매핑 생성
        ExhibitionArtistMap map = ExhibitionArtistMap.builder()
                .exhibition(exhibition)
                .artist(artist)
                .status(ExhibitionArtistStatus.JOINED)
                .build();

        exhibitionArtistMapRepository.save(map);

        // ⚠️ name 없으면 임시로 univName 사용
        return ExhibitionArtistAddResponse.of(
                exhibition.getId(),
                exhibition.getUnivName(),
                artist.getId(),
                artist.getNameKo()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExhibitionArtistListResponse> getArtistsByExhibition(UUID exhibitionId) {

        // 전시 존재 확인
        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        List<ExhibitionArtistMap> maps =
                exhibitionArtistMapRepository.findByExhibitionId(exhibitionId);

        return maps.stream()
                .map(map -> ExhibitionArtistListResponse.from(
                        map.getArtist().getId(),
                        map.getArtist().getNameKo()
                ))
                .toList();
    }

    // 전시 장소 정보 등록
    @Override
    public ExhibitionMapCreateResponse createExhibitionMap(UUID exhibitionId, ExhibitionMapCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (exhibitionMapRepository.existsByExhibitionId(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_ALREADY_EXISTS);
        }

        ExhibitionMap exhibitionMap = ExhibitionMap.builder()
                .exhibition(exhibition)
                .address(request.getAddress())
                .detailLocation(request.getDetailLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        ExhibitionMap saved = exhibitionMapRepository.save(exhibitionMap);

        return ExhibitionMapCreateResponse.builder()
                .mapId(saved.getId().toString())
                .exhibitionId(exhibition.getId().toString())
                .build();
    }

    // 전시 장소 정보 수정
    @Override
    public ExhibitionMapUpdateResponse updateExhibitionMap(UUID exhibitionId, ExhibitionMapUpdateRequest request) {
        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        ExhibitionMap exhibitionMap = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND));

        exhibitionMap.update(
                request.getAddress(),
                request.getDetailLocation(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ExhibitionMapUpdateResponse.builder()
                .mapId(exhibitionMap.getId().toString())
                .build();
    }

    // 전시 장소 정보 삭제
    @Override
    public void deleteExhibitionMap(UUID exhibitionId) {
        ExhibitionMap exhibitionMap = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND));

        exhibitionMapRepository.delete(exhibitionMap);
    }

    // 전시 작가 삭제
    @Override
    public ExhibitionArtistRemoveResponse removeArtistFromExhibition(UUID exhibitionId, UUID artistId) {

        ExhibitionArtistMap map = exhibitionArtistMapRepository
                .findByExhibitionIdAndArtistId(exhibitionId, artistId)
                .orElseThrow(() -> new ExhibitionArtistException(
                        ExhibitionErrorCode.EXHIBITION_ARTIST_NOT_FOUND
                ));

        Exhibition exhibition = map.getExhibition();
        Artist artist = map.getArtist();

        // 삭제 (or soft delete)
        exhibitionArtistMapRepository.delete(map);
        // map.updateStatus(ExhibitionArtistStatus.REMOVED);

        return ExhibitionArtistRemoveResponse.of(
                exhibition.getId(),
                exhibition.getUnivName(),
                artist.getId(),
                artist.getNameKo()
        );
    }
}