package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.entity.Host;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    private Integer publishedArtworkCount;
    private Integer hiddenArtworkCount;

    private String planName;

    private String address;
    private String hostName;
    private String hostDescription;

    // 게시(공개)에 필요한 필수값 중 비어있는 항목. 빈 배열이면 바로 게시 가능.
    // 가능한 값: title, exhibitionType, startDate, endDate, description, address, hostName, hostDescription
    private List<String> missingPublishFields;

    public static ExhibitionAdminHomeResponse of(
            Exhibition exhibition,
            ExhibitionDetail detail,
            String planName,
            long joinedArtistCount,
            long pendingArtistCount,
            long publishedArtworkCount,
            long hiddenArtworkCount,
            List<String> missingPublishFields,
            ExhibitionMap map,
            Host host
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
                .publishedArtworkCount((int) publishedArtworkCount)
                .hiddenArtworkCount((int) hiddenArtworkCount)
                .planName(planName)
                .address(map != null ? map.getAddress() : null)
                .hostName(host != null ? host.getName() : null)
                .hostDescription(host != null ? host.getDescription() : null)
                .missingPublishFields(missingPublishFields)
                .build();
    }
}
