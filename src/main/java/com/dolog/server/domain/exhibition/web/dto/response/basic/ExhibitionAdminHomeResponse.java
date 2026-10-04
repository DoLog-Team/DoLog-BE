package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class ExhibitionAdminHomeResponse {

    private String title;
    private ExhibitionType exhibitionType;
    private String univName;
    private String collegeName;
    private String deptName;
    private LocalDate startDate;
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    private String operationNotice;
    private String siteName;
    private String siteDescription;
    private String siteThumbnail;
    private String exhibitionUrl;
    private String artistJoinCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private Boolean isPublished;
    private Integer joinedArtistCount;
    private Integer pendingArtistCount;

    // TODO: 작품 공개/숨김 상태 필드가 생기면 실제 집계로 교체
    private Integer publishedArtworkCount;
    private Integer hiddenArtworkCount;

    private String planName;

    public static ExhibitionAdminHomeResponse of(
            Exhibition exhibition,
            ExhibitionDetail detail,
            String planName,
            long joinedArtistCount,
            long pendingArtistCount
    ) {
        return ExhibitionAdminHomeResponse.builder()
                .title(detail != null ? detail.getTitle() : null)
                .exhibitionType(exhibition.getExhibitionType())
                .univName(exhibition.getUnivName())
                .collegeName(exhibition.getCollegeName())
                .deptName(exhibition.getDeptName())
                .startDate(detail != null ? detail.getStartDate() : null)
                .endDate(detail != null ? detail.getEndDate() : null)
                .openTime(detail != null ? detail.getOpenTime() : null)
                .closeTime(detail != null ? detail.getCloseTime() : null)
                .operationNotice(detail != null ? detail.getOperationNotice() : null)
                .siteName(detail != null ? detail.getTitle() : null)
                .siteDescription(detail != null ? detail.getDescription() : null)
                .siteThumbnail(detail != null ? detail.getExhibitionImg() : null)
                .exhibitionUrl("https://dolog.kr/" + exhibition.getSlug())
                .artistJoinCode(exhibition.getArtistJoinCode())
                .publishedAt(exhibition.getPublishedAt())
                .expiresAt(exhibition.getExpiresAt())
                .isPublished(exhibition.isPublic())
                .joinedArtistCount((int) joinedArtistCount)
                .pendingArtistCount((int) pendingArtistCount)
                .publishedArtworkCount(0)
                .hiddenArtworkCount(0)
                .planName(planName)
                .build();
    }
}
