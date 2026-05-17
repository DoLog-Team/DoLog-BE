package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.exhibition.ArtworkExhibitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ArtworkMetaLoader {

    private final ArtworkArtistService artworkArtistService;
    private final ArtworkExhibitionService artworkExhibitionService;

    public ArtworkMetaData load(List<Artwork> artworks) {

        List<UUID> artworkIds = artworks.stream()
                .map(Artwork::getId)
                .toList();

        List<UUID> exhibitionIds = artworks.stream()
                .map(a -> a.getExhibition().getId())
                .distinct()
                .toList();

        Map<UUID, String> artistMap =
                artworkArtistService.fetchArtistMap(artworkIds);

        Map<UUID, String> exhibitionMap =
                artworkExhibitionService.fetchExhibitionDetailMap(exhibitionIds);

        return new ArtworkMetaData(
                artistMap,
                exhibitionMap
        );
    }

    public record ArtworkMetaData(
            Map<UUID, String> artistMap,
            Map<UUID, String> exhibitionMap
    ) {}
}