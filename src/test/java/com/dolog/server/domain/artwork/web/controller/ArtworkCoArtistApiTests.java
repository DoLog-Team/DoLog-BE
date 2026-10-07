package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkCoArtistApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionArtistMapRepository participations;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    private Exhibition exhibition;
    private Artist me;
    private String myToken;
    private ArtistProfile myProfile;
    private Artist colleague;
    private ArtistProfile colleagueProfile;

    @BeforeEach
    void setUp() {
        exhibition = exhibition();
        me = artist("나작가");
        myToken = token(me.getAccount());
        myProfile = join(me, exhibition, ExhibitionArtistStatus.JOINED);
        colleague = artist("동료작가");
        colleagueProfile = join(colleague, exhibition, ExhibitionArtistStatus.JOINED);
    }

    @Test
    @DisplayName("출품된 본인 작품에 같은 전시 참여 작가를 공동 작가로 등록하면 201 이고 작가 목록에 함께 나온다")
    void addsCoArtist() throws Exception {
        Artwork artwork = submittedArtwork();

        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(colleagueProfile.getId(), " 공동 작가 ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.artistId").value(colleague.getId().toString()))
                .andExpect(jsonPath("$.data.artistProfileId").value(colleagueProfile.getId().toString()))
                .andExpect(jsonPath("$.data.artistName").value("동료작가"))
                .andExpect(jsonPath("$.data.artistRole").value("공동 작가"))
                .andExpect(jsonPath("$.data.artwork.artists", hasSize(2)))
                .andExpect(jsonPath("$.data.exhibition.id").value(exhibition.getId().toString()));

        endRequest();
        assertTrue(artworks.findById(artwork.getId()).orElseThrow().isLinkedTo(colleague.getId()));
    }

    @Test
    @DisplayName("출품 전 작품에는 공동 작가를 등록할 수 없다 (예전엔 NPE 500)")
    void rejectsUnsubmittedArtwork() throws Exception {
        Artwork artwork = artworkOf(me, null);

        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(colleagueProfile.getId(), "공동 작가")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_010"));
    }

    @Test
    @DisplayName("다른 전시의 프로필이나 제외된 작가의 프로필은 400")
    void rejectsProfileOutsideExhibition() throws Exception {
        Artwork artwork = submittedArtwork();
        ArtistProfile otherExhibitionProfile = join(artist("다른 전시 작가"), exhibition(), ExhibitionArtistStatus.JOINED);
        ArtistProfile removedProfile = join(artist("제외된 작가"), exhibition, ExhibitionArtistStatus.REMOVED);

        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(otherExhibitionProfile.getId(), "공동 작가")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_016"));
        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(removedProfile.getId(), "공동 작가")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_016"));
    }

    @Test
    @DisplayName("이미 연결된 작가는 409, 없는 프로필은 404, 역할이 비면 400")
    void rejectsDuplicatesMissingAndBlank() throws Exception {
        Artwork artwork = submittedArtwork();

        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(myProfile.getId(), "또 나")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTWORK_017"));
        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(UUID.randomUUID(), "공동 작가")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_PROFILE_404"));
        mvc.perform(withToken(post("/api/artworks/{id}/artists", artwork.getId()), myToken)
                        .content(body(colleagueProfile.getId(), " ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("남의 작품은 404, 작가 어드민이 아니면 403")
    void accessRules() throws Exception {
        Artwork othersArtwork = artworkOf(colleague, colleagueProfile);
        Account admin = accounts.saveAndFlush(Account.builder().role(Role.DOLOG_ADMIN).accountStatus(AccountStatus.ACTIVE).build());

        mvc.perform(withToken(post("/api/artworks/{id}/artists", othersArtwork.getId()), myToken)
                        .content(body(myProfile.getId(), "끼워넣기")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        mvc.perform(withToken(post("/api/artworks/{id}/artists", othersArtwork.getId()), token(admin))
                        .content(body(myProfile.getId(), "관리자")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("artistId 로 역할을 수정하고, 공동 작가도 수정할 수 있다")
    void updatesRoleByArtistId() throws Exception {
        Artwork artwork = submittedArtwork();
        link(artwork, colleague, colleagueProfile, "공동 작가");

        mvc.perform(withToken(patch("/api/artworks/{id}/artists/{artistId}", artwork.getId(), colleague.getId()), token(colleague.getAccount()))
                        .content("{\"artistRole\":\"총괄 디렉터\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artistRole").value("총괄 디렉터"));
        mvc.perform(withToken(patch("/api/artworks/{id}/artists/{artistId}", artwork.getId(), UUID.randomUUID()), myToken)
                        .content("{\"artistRole\":\"없는 사람\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_006"));
    }

    @Test
    @DisplayName("프로필이 없는 작가가 연결된 작품도 수정 응답이 나온다 (예전엔 NPE 500)")
    void responseHandlesArtistWithoutProfile() throws Exception {
        Artwork artwork = submittedArtwork();
        Artist noProfile = artist("프로필 없는 작가");
        link(artwork, noProfile, null, "기획");

        mvc.perform(withToken(patch("/api/artworks/{id}/artists/{artistId}", artwork.getId(), noProfile.getId()), myToken)
                        .content("{\"artistRole\":\"총괄 기획\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artistProfileId").doesNotExist())
                .andExpect(jsonPath("$.data.artistName").value("프로필 없는 작가"))
                .andExpect(jsonPath("$.data.artwork.artists", hasSize(2)));
    }

    @Test
    @DisplayName("공동 작가를 해제하면 연결이 지워지고, 마지막 남은 작가는 해제할 수 없다")
    void removesCoArtistButNotLastOne() throws Exception {
        Artwork artwork = submittedArtwork();
        link(artwork, colleague, colleagueProfile, "공동 작가");

        mvc.perform(withToken(delete("/api/artworks/{id}/artists/{artistId}", artwork.getId(), colleague.getId()), myToken))
                .andExpect(status().isOk());
        endRequest();
        assertFalse(artworks.findById(artwork.getId()).orElseThrow().isLinkedTo(colleague.getId()));

        mvc.perform(withToken(delete("/api/artworks/{id}/artists/{artistId}", artwork.getId(), me.getId()), myToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_018"));
    }


    @Test
    @DisplayName("연결되지 않은 작가는 남의 작품의 역할을 수정하거나 작가를 해제할 수 없다 (404)")
    void cannotEditOthersArtworkMappings() throws Exception {
        Artwork artwork = submittedArtwork();
        String outsiderToken = token(artist("외부 작가").getAccount());

        mvc.perform(withToken(patch("/api/artworks/{id}/artists/{artistId}", artwork.getId(), me.getId()), outsiderToken)
                        .content("{\"artistRole\":\"탈취\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        mvc.perform(withToken(delete("/api/artworks/{id}/artists/{artistId}", artwork.getId(), me.getId()), outsiderToken))
                .andExpect(status().isNotFound());
    }

    // ---------------- helpers ----------------

    private Account account(Role role) {
        return accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
    }

    private Artist artist(String name) {
        return artists.saveAndFlush(Artist.builder().account(account(Role.ARTIST_ADMIN)).nameKo(name).build());
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private Exhibition exhibition() {
        return exhibitions.saveAndFlush(Exhibition.builder().account(account(Role.EXHIBITION_ADMIN))
                .univName("대학").deptName("학과").slug("coartist-" + UUID.randomUUID()).build());
    }

    private ArtistProfile join(Artist artist, Exhibition exhibition, ExhibitionArtistStatus status) {
        participations.saveAndFlush(ExhibitionArtistMap.builder().exhibition(exhibition).artist(artist).status(status).build());
        return profiles.saveAndFlush(ArtistProfile.builder().artist(artist).exhibition(exhibition).nameKo(artist.getNameKo()).build());
    }

    private Artwork artworkOf(Artist owner, ArtistProfile profile) {
        Artwork artwork = Artwork.builder().title("작품").description("설명").build();
        if (profile != null) {
            artwork.submitTo(profile.getExhibition(), null);
        }
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(owner).artistProfile(profile).artistRole("작가").build());
        return artworks.saveAndFlush(artwork);
    }

    private Artwork submittedArtwork() {
        return artworkOf(me, myProfile);
    }

    private void link(Artwork artwork, Artist artist, ArtistProfile profile, String role) {
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistProfile(profile).artistRole(role).build());
        artworks.saveAndFlush(artwork);
    }

    private String body(UUID profileId, String role) {
        return "{\"artistProfileId\":\"" + profileId + "\",\"artistRole\":\"" + role + "\"}";
    }

    private <T extends MockHttpServletRequestBuilder> T withToken(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
        return builder;
    }

    private void endRequest() {
        em.flush();
        em.clear();
    }
}
