package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.bts.entity.Bts;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtistProfileDetailResponse {
    private UUID profileId;
    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String profileImage;
    private boolean isPublic;
    private String bio;
    private ContactInfo contact;
    private List<BtsSummary> behindTheScenes;
    private List<ArtworkSummary> artworks;
    private NeighborArtist prevArtist;
    private NeighborArtist nextArtist;

    @Getter
    @Builder
    public static class ContactInfo {
        private String email;
        private List<SnsInfo> snsList;
    }

    @Getter
    @Builder
    public static class SnsInfo {
        private UUID snsId;
        private String platformName;
        private String url;
    }

    @Getter
    @Builder
    public static class BtsSummary {
        private UUID btsId;
        private String title;
        private String mainImg;
    }

    @Getter
    @Builder
    public static class ArtworkSummary {
        private UUID artworkId;
        private String title;
        private String image;
    }

    @Getter
    @Builder
    public static class NeighborArtist {
        private UUID id;
        private String name;
    }
}