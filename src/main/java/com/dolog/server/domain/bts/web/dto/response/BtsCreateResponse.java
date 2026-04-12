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
public class BtsCreateResponse {
    private UUID btsId;
    private String title;
    private String contentUrl;
    private String mainImg;

    // 연결된 전시 및 작가 정보
    private UUID exhibitionId;
    private ArtistInfo artist;

    // 연결된 작품 목록
    private List<ArtworkInfo> artworks;

    @Getter
    @Builder
    public static class ArtistInfo {
        private UUID id;
        private String name;
    }

    @Getter
    @Builder
    public static class ArtworkInfo {
        private UUID id;
        private String title;
    }

    public static BtsCreateResponse of(Bts bts) {
        return BtsCreateResponse.builder()
                .btsId(bts.getId())
                .title(bts.getTitle())
                .contentUrl(bts.getContentUrl())
                .mainImg(bts.getMainImg())
                .exhibitionId(bts.getExhibition() != null ? bts.getExhibition().getId() : null)
                .artist(bts.getArtist() != null ? ArtistInfo.builder()
                        .id(bts.getArtist().getId())
                        .name(bts.getArtist().getNameKo()) // 필드명에 맞춰 수정하세요 (nameKo 등)
                        .build() : null)
                .artworks(bts.getArtworkMaps().stream()
                        .map(map -> ArtworkInfo.builder()
                                .id(map.getArtwork().getId())
                                .title(map.getArtwork().getTitle())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
