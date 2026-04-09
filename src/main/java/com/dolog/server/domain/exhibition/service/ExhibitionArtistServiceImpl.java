package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.exception.ExhibitionArtistException;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistRemoveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionArtistServiceImpl implements ExhibitionArtistService{


    private final ArtistRepository artistRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

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
