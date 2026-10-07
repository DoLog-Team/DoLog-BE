package com.dolog.server.domain.artist.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ArtistPublicResponse {

    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String bio;
    private String profileImg;
    private String email;
    private List<SnsItem> snsList;
    private List<ExhibitionItem> exhibitions;
    private List<ArtworkItem> artworks;
    private int likeCount;
    private boolean liked;
    private long viewCount;

    public record SnsItem(
            String platformName,
            String url
    ) {
    }

    public record ExhibitionItem(
            UUID exhibitionId,
            String title,
            String slug,
            String exhibitionImg,
            String univName,
            String deptName,
            String location,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record ArtworkItem(
            UUID artworkId,
            String title,
            String mainImg
    ) {
    }
}
