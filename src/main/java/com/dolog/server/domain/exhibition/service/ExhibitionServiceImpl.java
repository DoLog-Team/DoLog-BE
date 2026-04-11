package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionDetailUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.basic.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    @Transactional(readOnly = true)
    public ExhibitionIntegratedResponse getExhibitionDetails(UUID exhibitionId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.isPublic()) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_PUBLIC);
        }

        ExhibitionDetail detail = exhibitionDetailRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND));

        ExhibitionMap map = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND));

        return ExhibitionIntegratedResponse.of(detail, map);
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


    @Override
    public ExhibitionDetailUpsertResponse upsertExhibitionDetail(UUID exhibitionId, ExhibitionDetailUpsertRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        Optional<ExhibitionDetail> exhibitionDetailOpt = exhibitionDetailRepository.findByExhibitionId(exhibitionId);
        boolean isNew = exhibitionDetailOpt.isEmpty();

        ExhibitionDetail exhibitionDetail = exhibitionDetailOpt.orElseGet(() -> ExhibitionDetail.builder()
                .exhibition(exhibition)
                .title(request.getTitle())
                .build());

        exhibitionDetail.updateBasicInfo(
                request.getTitle(),
                request.getDescription(),
                request.getExhibitionImg(),
                request.getStartDate(),
                request.getEndDate()
        );

        exhibitionDetailRepository.save(exhibitionDetail);

        String message = isNew ? "상세 정보가 성공적으로 등록되었습니다." : "상세 정보가 성공적으로 수정되었습니다.";

        return ExhibitionDetailUpsertResponse.builder()
                .exhibitionId(exhibition.getId())
                .message(message)
                .build();
    }
}
