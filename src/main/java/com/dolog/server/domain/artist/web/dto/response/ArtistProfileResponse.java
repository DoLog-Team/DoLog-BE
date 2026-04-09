package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionListItemResponse;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfileResponse {
    private UUID profileId;
    private String nameKo;
    private String nameEn;
    private String bio;
    private String email;
    private String profileImg; // 파일 경로 또는 URL

    // SNS
    private List<ArtistSnsResponse> snsList;

    // 전시회 정보
    private ExhibitionListItemResponse exhibition;
}