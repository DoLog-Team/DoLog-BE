package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.service.artwork.query.mapper.ArtworkResponseMapper;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkAdminItemResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkEditResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkManageItemResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistGlobalItemResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtistNameResponseTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "공통 프로필 이름")
    void artistAndExhibitionResponsesUseProfileNameIncludingNull(String name) {
        Artist artist = artist(name);

        assertEquals(name, ArtistResponse.from(artist).getNameKo());
        assertEquals(name, ArtistListItemResponse.from(artist).getNameKo());
        assertEquals(name, ArtistResponse.from(artist).getNameEn());
        assertEquals(name, ArtistListItemResponse.from(artist).getNameEn());
        var participation = ExhibitionArtistMap.builder()
                .artist(artist).exhibition(Exhibition.builder().build()).build();
        assertEquals(name, ExhibitionArtistGlobalItemResponse.from(participation, "전시").getNameKo());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "공통 프로필 이름")
    void artworkResponsesUseProfileEvenWhenMappingHasNoProfileLink(String name) {
        Artist artist = artist(name);
        Artwork artwork = Artwork.builder().build();
        ArtworkArtistMap map = ArtworkArtistMap.builder()
                .artist(artist).artwork(artwork).artistRole("작가").build();
        artwork.getArtworkArtistMaps().add(map);

        assertEquals(name, ArtworkEditResponse.ArtistInfo.from(map).getNameKo());
        assertEquals(name, ArtworkAdminItemResponse.from(artwork, null).getArtistNames().get(0));
        assertEquals(name, ArtworkManageItemResponse.of(artwork, 100).getArtistNames().get(0));
        var mapper = new ArtworkResponseMapper();
        assertEquals(name, mapper.toParticipantInfo(map).getNameKo());
        assertEquals(name, mapper.toParticipantInfo(map).getNameEn());
        assertEquals(name == null ? "" : name,
                mapper.toRelatedArtworkInfo(artwork, "next").getArtistName());

        map.linkProfile(artist.getProfile());
        assertEquals(name, mapper.toParticipantInfo(map).getNameKo());
        assertEquals(name == null ? "" : name,
                mapper.toRelatedArtworkInfo(artwork, "next").getArtistName());
    }

    private Artist artist(String name) {
        Artist artist = Artist.builder().nameKo("기존 기본 이름").nameEn("Old Name").build();
        artist.linkProfile(ArtistProfile.builder().artist(artist).nameKo(name).nameEn(name).build());
        return artist;
    }
}
