package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionCustomTheme;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionCustomThemeRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionDetailUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.basic.*;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
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
    private final ExhibitionCustomThemeRepository exhibitionCustomThemeRepository;
    private final ArtistRepository artistRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final FileService fileService;

    @Override
    @Transactional(readOnly = true)
    public ExhibitionMainResponse getMainExhibitions(String sort) {

        LocalDate today = LocalDate.now();
        List<Exhibition> exhibitions;

        if ("RANDOM".equalsIgnoreCase(sort)) {

            List<Exhibition> list =
                    exhibitionRepository.findOngoingExhibitions(today, PageRequest.of(0, 20));

            Collections.shuffle(list);

            exhibitions = list.stream().limit(3).toList();

        } else {

            exhibitions =
                    exhibitionRepository.findOngoingExhibitions(today, PageRequest.of(0, 3));
        }

        List<ExhibitionListItemResponse> items = exhibitions.stream()
                .map(e -> ExhibitionListItemResponse.of(e, e.getExhibitionDetail()))
                .toList();

        return ExhibitionMainResponse.builder()
                .mainExhibitions(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExhibitionListItemResponse> getExhibitions(Boolean isPublic, String univName, String search) {
        List<Exhibition> exhibitions = exhibitionRepository.findExhibitions(isPublic, univName, search);
        return exhibitions.stream()
                .map(e -> ExhibitionListItemResponse.of(e, e.getExhibitionDetail()))
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
    @Transactional(readOnly = true)
    public ExhibitionFooterResponse getFooterInfo(UUID exhibitionId) {
        Optional<ExhibitionDetail> detail = exhibitionDetailRepository.findByExhibitionId(exhibitionId);
        if (detail.isPresent()) {
            return ExhibitionFooterResponse.from(detail.get());
        }
        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }
        throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND);
    }

    @Override
    @Transactional(readOnly = true)
    public ExhibitionCustomThemeResponse getCustomTheme(UUID exhibitionId) {
        ExhibitionCustomTheme theme = exhibitionCustomThemeRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> {
                    // 테마 미존재 시 전시회 존재 여부로 에러 구분
                    if (!exhibitionRepository.existsById(exhibitionId)) {
                        return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
                    }
                    return new ExhibitionException(ExhibitionErrorCode.CUSTOM_THEME_NOT_FOUND);
                });

        String splashImg = exhibitionDetailRepository.findByExhibitionId(exhibitionId)
                .map(ExhibitionDetail::getSplashImg)
                .orElse(null);

        return ExhibitionCustomThemeResponse.of(theme, exhibitionId, splashImg);
    }

    @Override
    @Transactional(readOnly = true)
    public ExhibitionMetaResponse getExhibitionMeta(UUID exhibitionId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        ExhibitionDetail detail = exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND));

        return ExhibitionMetaResponse.of(exhibition, detail);
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
    public ExhibitionDetailUpsertResponse upsertExhibitionDetail(UUID exhibitionId, ExhibitionDetailUpsertRequest request) throws IOException {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        String imageUrl = fileService.uploadFile(request.getExhibitionImg(), "exhibitions");
        if (imageUrl == null) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_IMAGE_REQUIRED);
        }

        Optional<ExhibitionDetail> exhibitionDetailOpt = exhibitionDetailRepository.findByExhibitionId(exhibitionId);
        boolean isNew = exhibitionDetailOpt.isEmpty();

        if (!isNew) {
            fileService.deleteFile(exhibitionDetailOpt.get().getExhibitionImg());
        }

        ExhibitionDetail exhibitionDetail = exhibitionDetailOpt.orElseGet(() -> ExhibitionDetail.builder()
                .exhibition(exhibition)
                .title(request.getTitle())
                .build());

        exhibitionDetail.updateBasicInfo(
                request.getTitle(),
                request.getDescription(),
                imageUrl,
                request.getStartDate(),
                request.getEndDate(),
                request.getDateInfo(),
                request.getEmail(),
                request.getLocationDescription()
        );

        exhibitionDetailRepository.save(exhibitionDetail);

        String message = isNew ? "상세 정보가 성공적으로 등록되었습니다." : "상세 정보가 성공적으로 수정되었습니다.";

        return ExhibitionDetailUpsertResponse.builder()
                .exhibitionId(exhibition.getId())
                .message(message)
                .build();
    }
}
