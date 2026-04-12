package com.dolog.server.domain.bts.web.dto.response;

import com.dolog.server.domain.bts.entity.Bts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class BtsListResponse {
    private UUID btsId;
    private String thumbnail;
    private String title;
    private List<String> artistNames;
    private List<String> artworkTitles;

    public static BtsListResponse from(Bts bts) {
        return BtsListResponse.builder()
                .btsId(bts.getId())
                .thumbnail(bts.getMainImg())
                .title(bts.getTitle())
                // 작가 이름 리스트화
                .artistNames(bts.getArtist() != null ?
                        List.of(bts.getArtist().getNameKo()) : List.of())
                // 작품 제목 리스트 추출
                .artworkTitles(bts.getArtworkMaps().stream()
                        .map(map -> map.getArtwork().getTitle())
                        .collect(Collectors.toList()))
                .build();
    }
}
