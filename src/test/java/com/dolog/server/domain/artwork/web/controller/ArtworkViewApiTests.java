package com.dolog.server.domain.artwork.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.RefreshToken;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.RefreshTokenRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkViewApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;

    private Artist me;
    private Artwork artwork;

    @BeforeEach
    void setUp() {
        me = artist("나작가");
        artwork = artworkOf(me, ArtworkStatus.PUBLISHED, exhibition(true));
        endRequest();
    }

    @Test
    @DisplayName("처음 온 방문자는 조회수가 1 오르고 visitor_id 쿠키를 받는다")
    void countsFirstViewAndIssuesCookie() throws Exception {
        mvc.perform(api(view(artwork)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artworkId").value(artwork.getId().toString()))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.visitorId").value(startsWith("vis_")))
                .andExpect(cookie().exists("visitor_id"));
        endRequest();

        assertEquals(1L, viewCountOf(artwork));
    }

    @Test
    @DisplayName("같은 방문자가 다시 보면 세지 않고, 다른 방문자는 센다")
    void countsOncePerVisitor() throws Exception {
        mvc.perform(api(view(artwork)).header("X-Visitor-Id", "vis_a"))
                .andExpect(jsonPath("$.data.counted").value(true));
        endRequest();
        mvc.perform(api(view(artwork)).header("X-Visitor-Id", "vis_a"))
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));
        endRequest();
        mvc.perform(api(view(artwork)).param("visitorId", "vis_b"))
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(2));
        endRequest();

        assertEquals(2L, viewCountOf(artwork));
    }

    @Test
    @DisplayName("상세 조회는 조회수를 올리지 않는다 (조회수는 이 API 로만 센다)")
    void detailDoesNotCount() throws Exception {
        mvc.perform(api(get("/api/artworks/{id}", artwork.getId()))).andExpect(status().isOk());
        mvc.perform(api(get("/api/exhibitions/{e}/artworks/{a}",
                artwork.getExhibition().getId(), artwork.getId()))).andExpect(status().isOk());
        endRequest();

        assertEquals(0L, viewCountOf(artwork));
    }

    @Test
    @DisplayName("작가 본인이 로그인해서 봐도 센다 (방문자 기준으로만 중복 제거)")
    void countsOwnArtworkToo() throws Exception {
        mvc.perform(api(view(artwork)).header("X-Visitor-Id", "vis_owner")
                        .header("Authorization", "Bearer " + token(me.getAccount())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(1));
    }

    @Test
    @DisplayName("다음 날 다시 와도 같은 방문자는 다시 세지 않는다 (작품마다 한 번)")
    void countsOncePerArtworkEvenLater() throws Exception {
        em.createNativeQuery("INSERT INTO artwork_view_logs (artwork_id, visitor_id, created_at) "
                        + "VALUES (:id, 'vis_yesterday', NOW(6) - INTERVAL 2 DAY)")
                .setParameter("id", artwork.getId()).executeUpdate();

        mvc.perform(api(view(artwork)).header("X-Visitor-Id", "vis_yesterday"))
                .andExpect(jsonPath("$.data.counted").value(false));
    }

    @Test
    @DisplayName("두록 URL 에 안 보이는 작품(비공개, 게시 전 전시, 삭제, 없음)은 404, 숨김 작품은 센다")
    void onlyCountsVisibleArtworks() throws Exception {
        Artwork draft = artworkOf(me, ArtworkStatus.DRAFT, exhibition(true));
        Artwork unpublishedExhibition = artworkOf(me, ArtworkStatus.PUBLISHED, exhibition(false));
        Artwork deleted = artworkOf(me, ArtworkStatus.PUBLISHED, exhibition(true));
        deleted.markDeleted(LocalDateTime.now());
        Artwork hidden = artworkOf(me, ArtworkStatus.PUBLISHED, exhibition(true));
        hidden.hide(LocalDateTime.now());
        endRequest();

        for (UUID id : new UUID[]{draft.getId(), unpublishedExhibition.getId(), deleted.getId(), UUID.randomUUID()}) {
            mvc.perform(api(post("/api/artworks/{id}/views", id)).header("X-Visitor-Id", "vis_x"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        }
        mvc.perform(api(view(hidden)).header("X-Visitor-Id", "vis_x"))
                .andExpect(jsonPath("$.data.counted").value(true));
    }

    // ---------------- helpers ----------------

    private long viewCountOf(Artwork artwork) {
        return artworks.findById(artwork.getId()).orElseThrow().getViewCount();
    }

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

    private Exhibition exhibition(boolean published) {
        return exhibitions.saveAndFlush(Exhibition.builder().account(account(Role.EXHIBITION_ADMIN))
                .univName("테스트 대학").deptName("테스트 학과").slug("view-" + UUID.randomUUID())
                .isPublic(published).build());
    }

    private Artwork artworkOf(Artist artist, ArtworkStatus status, Exhibition exhibition) {
        Artwork artwork = Artwork.builder().title("작품").category("회화").description("설명")
                .exhibition(exhibition).build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        artwork.changeStatus(status);
        return artworks.saveAndFlush(artwork);
    }

    private MockHttpServletRequestBuilder view(Artwork artwork) {
        return post("/api/artworks/{id}/views", artwork.getId());
    }

    private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder builder) {
        return builder.contextPath("/api");
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
