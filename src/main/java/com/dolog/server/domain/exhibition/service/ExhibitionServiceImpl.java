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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                    exhibitionRepository.findLatestExhibitions(today);

            Collections.shuffle(list);

            exhibitions = list.stream()
                    .limit(3)
                    .toList();

        }
        else if ("LATEST".equalsIgnoreCase(sort)) {

            exhibitions =
                    exhibitionRepository.findLatestExhibitions(today)
                            .stream()
                            .limit(3)
                            .toList();
        }
        else {

            List<Exhibition> list =
                    exhibitionRepository.findDefaultExhibitions(today);

            exhibitions = list.stream()
                    .sorted((e1, e2) -> {
                        LocalDate s1 = e1.getExhibitionDetail().getStartDate();
                        LocalDate s2 = e2.getExhibitionDetail().getStartDate();

                        boolean ongoing1 = !s1.isAfter(today);
                        boolean ongoing2 = !s2.isAfter(today);

                        if (ongoing1 != ongoing2) {
                            return ongoing1 ? -1 : 1;
                        }

                        if (ongoing1) {
                            return s2.compareTo(s1);
                        }

                        return s1.compareTo(s2);
                    })
                    .limit(3)
                    .toList();
        }

        List<ExhibitionListItemResponse> items = exhibitions.stream()
                .map(e -> {
                    ExhibitionDetail d = e.getExhibitionDetail();

                    Long dDay = null;
                    if (d.getStartDate() != null) {
                        dDay = ChronoUnit.DAYS.between(today, d.getStartDate());
                    }

                    return ExhibitionListItemResponse.of(e, d, today)
                            .toBuilder()
                            .dDay(dDay)
                            .build();
                })
                .toList();

        return ExhibitionMainResponse.builder()
                .mainExhibitions(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExhibitionListItemResponse> getExhibitions(
            Boolean isPublic,
            String univName,
            String search
    ) {
        LocalDate today = LocalDate.now();

        List<Exhibition> exhibitions =
                exhibitionRepository.findExhibitions(isPublic, univName, search);

        return exhibitions.stream()
                .map(e -> ExhibitionListItemResponse.of(
                        e,
                        e.getExhibitionDetail(),
                        today
                ))
                .toList();
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
                .orElse(null);

        return ExhibitionIntegratedResponse.of(detail, map);
    }

    @Override
    @Transactional(readOnly = true)
    public ExhibitionFooterResponse getFooterInfo(UUID exhibitionId) {
        ExhibitionDetail detail = exhibitionDetailRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> {
                    if (!exhibitionRepository.existsById(exhibitionId)) {
                        return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
                    }
                    return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_DETAIL_NOT_FOUND);
                });

        ExhibitionMap map = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND));

        return ExhibitionFooterResponse.from(detail, map);
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
    @Transactional(readOnly = true)
    public ExhibitionResolveResponse resolveSlug(String slug) {
        Exhibition exhibition = exhibitionRepository.findBySlug(slug)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_SLUG_NOT_FOUND));
        return ExhibitionResolveResponse.of(exhibition.getId());
    }

    @Override
    public ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request) {
        if (exhibitionRepository.existsBySlug(request.getSlug())) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_SLUG_DUPLICATE);
        }

        Exhibition exhibition = Exhibition.builder()
                .account(null) //TODO: JWT -> v2 에서 연동함
                .univName(request.getUnivName())
                .deptName(request.getDeptName())
                .slug(request.getSlug())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        Exhibition saved = exhibitionRepository.save(exhibition);

        return ExhibitionCreateResponse.builder()
                .id(saved.getId().toString())
                .slug(saved.getSlug())
                .message("전시회가 성공적으로 등록되었습니다.")
                .build();
    }

    @Override
    public ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (request.getSlug() != null) {
            if (request.getSlug().isBlank()) {
                throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_SLUG_INVALID);
            }
            if (!request.getSlug().equals(exhibition.getSlug())
                    && exhibitionRepository.existsBySlug(request.getSlug())) {
                throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_SLUG_DUPLICATE);
            }
        }

        exhibition.updateBasicInfo(request.getUnivName(), request.getDeptName(), request.getSlug(), request.getIsPublic());

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
    @Transactional
    public ExhibitionDetailUpsertResponse upsertExhibitionDetail(
            UUID exhibitionId,
            ExhibitionDetailUpsertRequest request
    ) throws IOException {

        // 1. 전시 조회
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. 기존 상세 조회
        Optional<ExhibitionDetail> exhibitionDetailOpt =
                exhibitionDetailRepository.findByExhibitionId(exhibitionId);

        boolean isNew = exhibitionDetailOpt.isEmpty();

        ExhibitionDetail exhibitionDetail = exhibitionDetailOpt.orElseGet(() ->
                ExhibitionDetail.builder()
                        .exhibition(exhibition)
                        .build()
        );

        // 3. 이미지 처리
        String imageUrl = exhibitionDetail.getExhibitionImg(); // 기본: 기존 이미지 유지

        if (request.getExhibitionImg() != null && !request.getExhibitionImg().isEmpty()) {

            // 새 이미지 업로드
            String newImageUrl = fileService.uploadFile(request.getExhibitionImg(), "exhibitions");

            if (newImageUrl == null) {
                throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_IMAGE_REQUIRED);
            }

            // 기존 이미지 삭제 (기존 데이터 있을 때만)
            if (!isNew && exhibitionDetail.getExhibitionImg() != null) {
                fileService.deleteFile(exhibitionDetail.getExhibitionImg());
            }

            imageUrl = newImageUrl;
        }

        // 4. 신규 생성 시 필수값 체크
        if (isNew && imageUrl == null) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_IMAGE_REQUIRED);
        }

        // 로고 이미지 처리
        String logoImgUrl = null;
        if (request.getLogoImg() != null && !request.getLogoImg().isEmpty()) {
            try {
                String newLogoUrl = fileService.uploadFile(request.getLogoImg(), "logos");
                if (!isNew && exhibitionDetail.getLogoImg() != null) {
                    fileService.deleteFile(exhibitionDetail.getLogoImg());
                }
                logoImgUrl = newLogoUrl;
            } catch (IllegalArgumentException e) {
                throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_IMAGE_REQUIRED);
            }
        }

        // 5. 업데이트
        exhibitionDetail.updateBasicInfo(
                request.getTitle() != null ? request.getTitle() : exhibitionDetail.getTitle(),

                request.getDescription() != null
                        ? request.getDescription().replace("\\n", "\n")
                        : exhibitionDetail.getDescription(),

                imageUrl,

                request.getStartDate() != null
                        ? request.getStartDate()
                        : exhibitionDetail.getStartDate(),

                request.getEndDate() != null
                        ? request.getEndDate()
                        : exhibitionDetail.getEndDate(),

                request.getDateInfo() != null
                        ? request.getDateInfo().replace("\\n", "\n")
                        : exhibitionDetail.getDateInfo(),

                request.getEmail() != null
                        ? request.getEmail()
                        : exhibitionDetail.getEmail(),

                request.getCopyright() != null
                        ? request.getCopyright()
                        : exhibitionDetail.getCopyright(),

                logoImgUrl
        );

        // 6. 저장
        exhibitionDetailRepository.save(exhibitionDetail);

        // 7. 응답
        String message = isNew
                ? "상세 정보가 성공적으로 등록되었습니다."
                : "상세 정보가 성공적으로 수정되었습니다.";

        return ExhibitionDetailUpsertResponse.builder()
                .exhibitionId(exhibition.getId())
                .message(message)
                .build();
    }
}
