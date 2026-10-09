package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 두록 어드민 전용. 전시를 가로지르는 참여 신청/참여 작가 조회 항목.
@Getter
@Builder
public class ExhibitionArtistGlobalItemResponse {

    private UUID exhibitionId;
    private String exhibitionName;
    private UUID artistId;
    private String nameKo;
    private String email;
    private String greeting;
    private ExhibitionArtistStatus status;
    private LocalDateTime createdAt;

    public static ExhibitionArtistGlobalItemResponse from(ExhibitionArtistMap map, String exhibitionName) {
        return ExhibitionArtistGlobalItemResponse.builder()
                .exhibitionId(map.getExhibition().getId())
                .exhibitionName(exhibitionName)
                .artistId(map.getArtist().getId())
                .nameKo(map.getArtist().getNameKo())
                .email(map.getArtist().getAccount() != null ? map.getArtist().getAccount().getEmail() : null)
                .greeting(map.getGreeting())
                .status(map.getStatus())
                .createdAt(map.getCreatedAt())
                .build();
    }
}
