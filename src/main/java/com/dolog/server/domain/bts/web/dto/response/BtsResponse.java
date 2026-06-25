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
public class BtsResponse {
    private UUID btsId;
    private String title;
    private String linkLabel;
    private String linkUrl;
    private String content;
    private String mainImg;

    private UUID exhibitionId;
    private ArtistProfileInfo artistProfile;

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

    public static BtsResponse of(Bts bts) {
        var profile = bts.getArtistProfile();

        return BtsResponse.builder()
                .btsId(bts.getId())
                .title(bts.getTitle())
                .linkLabel(bts.getLinkLabel())
                .linkUrl(bts.getLinkUrl())
                .content(bts.getContent())
                .mainImg(bts.getMainImg())
                .exhibitionId(bts.getExhibition() != null ? bts.getExhibition().getId() : null)
                .artistProfile(profile != null ? ArtistProfileInfo.builder()
                        .id(profile.getId())
                        .name(profile.getNameKo())
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
