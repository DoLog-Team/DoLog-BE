package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class ArtistDetailArtworksApiTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;

    @Test
    @DisplayName("작가 상세 작품 목록에는 전시전용(exhibitionOnly) 작품은 빠지고 공개 작품만 담긴다")
    void excludesExhibitionOnlyArtworks() throws Exception {
        Account account = accounts.saveAndFlush(Account.builder()
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artists.saveAndFlush(Artist.builder().account(account).nameKo("작가").build());

        Artwork visible = artwork(artist, "공개 작품", false);
        artwork(artist, "전시전용 작품", true);

        mvc.perform(get("/api/artists/{artistId}", artist.getId()).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artworks[*].artworkId", contains(visible.getId().toString())));
    }

    private Artwork artwork(Artist artist, String title, boolean exhibitionOnly) {
        Artwork artwork = Artwork.builder().title(title).description("설명").category("카테고리")
                .exhibitionOnly(exhibitionOnly).build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }
}
