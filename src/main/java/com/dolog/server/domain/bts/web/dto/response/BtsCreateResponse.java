package com.dolog.server.domain.bts.web.dto.response;

import com.dolog.server.domain.artist.entity.ArtistProfile;
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
    private ArtistProfileInfo artistProfile;

    // 연결된 작품 목록
    private List<ArtworkInfo> artworks;

    @Getter
    @Builder
    public static class ArtistProfileInfo {
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
        // Artist -> ArtistProfile로 변경된 엔티티 구조 반영
        var profile = bts.getArtistProfile();

        return BtsCreateResponse.builder()
                .btsId(bts.getId())
                .title(bts.getTitle())
                .contentUrl(bts.getContentUrl())
                .mainImg(bts.getMainImg())
                .exhibitionId(bts.getExhibition() != null ? bts.getExhibition().getId() : null)
                .artistProfile(profile != null ? ArtistProfileInfo.builder()
                        .id(profile.getId()) // ArtistProfile의 ID
                        .name(profile.getNameKo()) // ArtistProfile의 한국어 이름
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
