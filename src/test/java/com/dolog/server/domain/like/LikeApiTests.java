package com.dolog.server.domain.like;

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
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class LikeApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtworkRepository artworks;
    @Autowired ArtworkLikeRepository artworkLikes;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionArtistMapRepository participations;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    private Artist artist;
    private Exhibition exhibition;
    private Artwork published;

    @BeforeEach
    void setUp() {
        artist = artists.saveAndFlush(Artist.builder().account(account(Role.ARTIST_ADMIN)).nameKo("작가").build());
        exhibition = exhibitions.saveAndFlush(Exhibition.builder().account(account(Role.EXHIBITION_ADMIN))
                .univName("대학").deptName("학과").slug("like-" + UUID.randomUUID()).build());
        published = artwork(ArtworkStatus.PUBLISHED);
    }

    @Test
    @DisplayName("처음 온 방문자가 좋아요를 누르면 visitor_id 를 발급해 쿠키와 응답으로 내려주고 201")
    void issuesVisitorIdOnFirstLike() throws Exception {
        MvcResult result = mvc.perform(likeArtwork(published.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED_201"))
                .andExpect(jsonPath("$.data.artworkId").value(published.getId().toString()))
                .andExpect(jsonPath("$.data.visitorId", startsWith("vis_")))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String visitorId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.visitorId");
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("visitor_id=" + visitorId));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.contains("Secure"));
    }

    @Test
    @DisplayName("HTTPS 요청이면 다른 도메인 FE 에서도 쿠키가 가도록 SameSite=None; Secure")
    void crossSiteCookieOnHttps() throws Exception {
        String setCookie = mvc.perform(likeArtwork(published.getId()).secure(true))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertTrue(setCookie.contains("SameSite=None"));
        assertTrue(setCookie.contains("Secure"));
    }

    @Test
    @DisplayName("같은 방문자가 다시 누르면 409 이고 좋아요 수는 그대로, 다른 방문자는 수가 늘어난다")
    void duplicateLikeIsConflict() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_a"))).andExpect(status().isCreated());

        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_a")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIKE_001"));
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_b")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeCount").value(2));
    }

    @Test
    @DisplayName("쿠키가 있으면 쿠키를 쓰고 Set-Cookie 를 다시 내리지 않는다")
    void cookieWinsOverBody() throws Exception {
        MvcResult result = mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_cookie"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visitorId\":\"vis_body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitorId").value("vis_cookie"))
                .andReturn();

        assertNull(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("쿠키가 없으면 본문의 visitorId(로컬스토리지 값)를 쓰고 그 값을 쿠키로 내려준다")
    void bodyVisitorIdWhenNoCookie() throws Exception {
        String setCookie = mvc.perform(likeArtwork(published.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visitorId\":\"vis_local\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitorId").value("vis_local"))
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertTrue(setCookie.startsWith("visitor_id=vis_local"));
    }

    @Test
    @DisplayName("본문의 visitorId 형식이 잘못되면 400")
    void rejectsInvalidVisitorId() throws Exception {
        mvc.perform(likeArtwork(published.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visitorId\":\"bad id!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIKE_003"));
    }

    @Test
    @DisplayName("비공개 작품이나 없는 작품에는 좋아요를 누를 수 없다")
    void cannotLikeHiddenOrMissingArtwork() throws Exception {
        Artwork draft = artwork(ArtworkStatus.DRAFT);

        mvc.perform(likeArtwork(draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        mvc.perform(likeArtwork(UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("좋아요를 취소하면 수가 줄고 liked=false, 다시 취소하면 404")
    void cancelArtworkLike() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_a"))).andExpect(status().isCreated());
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_b"))).andExpect(status().isCreated());

        mvc.perform(cancelArtwork(published.getId()).cookie(visitor("vis_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(false));
        mvc.perform(cancelArtwork(published.getId()).cookie(visitor("vis_a")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIKE_002"));
    }

    @Test
    @DisplayName("식별자 없이 취소하면 404 이고 새 visitor_id 를 발급하지 않는다")
    void cancelWithoutVisitorId() throws Exception {
        MvcResult result = mvc.perform(cancelArtwork(published.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIKE_002"))
                .andReturn();

        assertNull(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("작품이 비공개로 바뀌어도 이미 누른 좋아요는 취소할 수 있다")
    void cancelAfterArtworkBecameDraft() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_a"))).andExpect(status().isCreated());
        published.changeStatus(ArtworkStatus.DRAFT);
        em.flush();

        mvc.perform(cancelArtwork(published.getId()).cookie(visitor("vis_a")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("작품을 삭제하면 좋아요도 함께 지워진다")
    void deletingArtworkDeletesLikes() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_a"))).andExpect(status().isCreated());
        UUID id = published.getId();

        mvc.perform(delete("/api/artworks/{id}", id).contextPath("/api")
                        .header("Authorization", "Bearer " + token(artist.getAccount())))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        assertEquals(0L, artworkLikes.countByArtworkId(id));
        assertTrue(artworks.findById(id).isEmpty());
    }


    @Test
    @DisplayName("쿠키가 막힌 환경을 위해 X-Visitor-Id 헤더로 좋아요하고 취소할 수 있다 (DELETE 본문은 유실될 수 있음)")
    void headerVisitorIdForLikeAndCancel() throws Exception {
        mvc.perform(likeArtwork(published.getId()).header("X-Visitor-Id", "vis_header"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitorId").value("vis_header"));

        mvc.perform(cancelArtwork(published.getId()).header("X-Visitor-Id", "vis_header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    @DisplayName("visitorId 쿼리 파라미터로도 취소할 수 있다")
    void queryVisitorIdForCancel() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_query"))).andExpect(status().isCreated());

        mvc.perform(cancelArtwork(published.getId()).param("visitorId", "vis_query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("쿠키가 있으면 헤더보다 쿠키가 우선이고, 헤더 값 형식이 잘못되면 400")
    void cookieOverHeaderAndHeaderFormat() throws Exception {
        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_cookie")).header("X-Visitor-Id", "vis_header"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.visitorId").value("vis_cookie"));
        mvc.perform(likeArtwork(published.getId()).header("X-Visitor-Id", "bad id!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIKE_003"));
    }

    @Test
    @DisplayName("작품과 작가 좋아요의 응답 메시지는 같은 문체를 쓴다")
    void unifiedMessages() throws Exception {
        ArtistProfile profile = profile(ExhibitionArtistStatus.JOINED);

        mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_m")))
                .andExpect(jsonPath("$.message").value("좋아요가 등록되었습니다."));
        mvc.perform(cancelArtwork(published.getId()).cookie(visitor("vis_m")))
                .andExpect(jsonPath("$.message").value("좋아요가 취소되었습니다."));
        mvc.perform(withApi(post("/api/artist-profiles/{id}/likes", profile.getId())).cookie(visitor("vis_m")))
                .andExpect(jsonPath("$.message").value("좋아요가 등록되었습니다."));
        mvc.perform(withApi(delete("/api/artist-profiles/{id}/likes", profile.getId())).cookie(visitor("vis_m")))
                .andExpect(jsonPath("$.message").value("좋아요가 취소되었습니다."));
    }

    @Test
    @DisplayName("좋아요가 여러 개인 작품을 삭제해도 좋아요가 모두 지워진다")
    void deletingArtworkWithManyLikes() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(likeArtwork(published.getId()).cookie(visitor("vis_many_" + i))).andExpect(status().isCreated());
        }
        UUID id = published.getId();

        mvc.perform(delete("/api/artworks/{id}", id).contextPath("/api")
                        .header("Authorization", "Bearer " + token(artist.getAccount())))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        assertEquals(0L, artworkLikes.countByArtworkId(id));
    }

    @Test
    @DisplayName("참여 중인 작가의 프로필에 좋아요/취소할 수 있다")
    void likeAndCancelJoinedArtistProfile() throws Exception {
        ArtistProfile profile = profile(ExhibitionArtistStatus.JOINED);

        mvc.perform(withApi(post("/api/artist-profiles/{id}/likes", profile.getId())).cookie(visitor("vis_a")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.profileId").value(profile.getId().toString()))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.liked").value(true));
        mvc.perform(withApi(post("/api/artist-profiles/{id}/likes", profile.getId())).cookie(visitor("vis_a")))
                .andExpect(status().isConflict());
        mvc.perform(withApi(delete("/api/artist-profiles/{id}/likes", profile.getId())).cookie(visitor("vis_a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.liked").value(false));
    }

    @Test
    @DisplayName("제외되었거나 승인 대기 중인 작가의 프로필에는 좋아요를 누를 수 없다")
    void cannotLikeNotJoinedArtistProfile() throws Exception {
        ArtistProfile removed = profile(ExhibitionArtistStatus.REMOVED);

        mvc.perform(withApi(post("/api/artist-profiles/{id}/likes", removed.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_PROFILE_404"));
    }

    private Account account(Role role) {
        return accounts.saveAndFlush(Account.builder().role(role).accountStatus(AccountStatus.ACTIVE).build());
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private Artwork artwork(ArtworkStatus status) {
        Artwork artwork = Artwork.builder().title("작품").description("설명").build();
        artwork.changeStatus(status);
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private ArtistProfile profile(ExhibitionArtistStatus status) {
        participations.saveAndFlush(ExhibitionArtistMap.builder()
                .exhibition(exhibition).artist(artist).status(status).build());
        return profiles.saveAndFlush(ArtistProfile.builder()
                .artist(artist).exhibition(exhibition).nameKo("작가").build());
    }

    private MockHttpServletRequestBuilder likeArtwork(UUID artworkId) {
        return withApi(post("/api/artworks/{id}/likes", artworkId));
    }

    private MockHttpServletRequestBuilder cancelArtwork(UUID artworkId) {
        return withApi(delete("/api/artworks/{id}/likes", artworkId));
    }

    private MockHttpServletRequestBuilder withApi(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/api");
    }

    private Cookie visitor(String value) {
        return new Cookie("visitor_id", value);
    }
}
