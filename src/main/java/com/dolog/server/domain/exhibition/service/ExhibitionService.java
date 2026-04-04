package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionListItemResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionMainResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionMessageResponse;
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
public class ExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;

    @Transactional(readOnly = true)
    public ExhibitionMainResponse getMainExhibitions() {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findTop3PublicExhibitions(PageRequest.of(0, 3));
        List<ExhibitionListItemResponse> items = details.stream()
                .map(ExhibitionListItemResponse::from)
                .collect(Collectors.toList());
        return ExhibitionMainResponse.builder().mainExhibitions(items).build();
    }

    @Transactional(readOnly = true)
    public List<ExhibitionListItemResponse> getExhibitions(Boolean isPublic, String univName, String search) {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findExhibitions(isPublic, univName, search);
        return details.stream()
                .map(ExhibitionListItemResponse::from)
                .collect(Collectors.toList());
    }

    public ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request) {
        Exhibition exhibition = Exhibition.builder()
                .account(null) // TODO: JWT 연동 후 인증된 계정으로 교체
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

    public ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        exhibition.updateBasicInfo(request.getUnivName(), request.getDeptName(), request.getIsPublic());

        return ExhibitionMessageResponse.builder()
                .message("기본 정보가 성공적으로 수정되었습니다.")
                .build();
    }

    public ExhibitionMessageResponse deleteExhibition(UUID exhibitionId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        exhibitionRepository.delete(exhibition);

        return ExhibitionMessageResponse.builder()
                .message("전시회가 성공적으로 삭제되었습니다.")
                .build();
    }
}
