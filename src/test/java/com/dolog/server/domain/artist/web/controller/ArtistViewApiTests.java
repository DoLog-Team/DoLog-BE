package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class ArtistViewApiTests {

    @Autowired MockMvc mvc;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired AccountRepository accounts;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionDetailRepository exhibitionDetails;
    @Autowired ExhibitionArtistMapRepository exhibitionArtists;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("비로그인 첫 방문을 집계하고 발급한 쿠키로 재호출하면 중복 집계하지 않는다")
    void issuesCookieAndCountsOnlyFirstVisit() throws Exception {
        ArtistProfile profile = createProfile(0);
        UUID artistId = profile.getArtist().getId();

        Cookie visitorCookie = mvc.perform(recordView(artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artistId").value(artistId.toString()))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.visitorId").value(startsWith("vis_")))
                .andExpect(cookie().exists("visitor_id"))
                .andReturn().getResponse().getCookie("visitor_id");

        mvc.perform(recordView(artistId).cookie(visitorCookie).header("X-Visitor-Id", "different"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visitorId").value(visitorCookie.getValue()))
                .andExpect(jsonPath("$.data.counted").value(false))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mvc.perform(recordView(artistId).header("X-Visitor-Id", "different"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counted").value(true))
                .andExpect(jsonPath("$.data.viewCount").value(2));
        assertEquals(2L, profiles.findViewCountById(profile.getId()));
    }

    @Test
    @DisplayName("같은 방문자도 작가가 다르면 각각 집계한다")
    void countsOncePerArtist() throws Exception {
        ArtistProfile first = createProfile(0);
        ArtistProfile second = createProfile(0);

        for (ArtistProfile profile : new ArtistProfile[]{first, second}) {
            mvc.perform(recordView(profile.getArtist().getId()).header("X-Visitor-Id", "same_visitor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.counted").value(true))
                    .andExpect(jsonPath("$.data.viewCount").value(1));
        }
    }

    @Test
    @DisplayName("두록 GET은 조회수를 유지하고 두록 POST와 전시 GET은 공통 조회수에 합산한다")
    void sharesViewCountWithExhibition() throws Exception {
        ArtistProfile profile = createProfile(10);
        UUID artistId = profile.getArtist().getId();
        UUID exhibitionId = createExhibition(profile.getArtist());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(getArtist(artistId).header("X-Visitor-Id", "first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(10));
        mvc.perform(getArtist(artistId).header("X-Visitor-Id", "second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(10));

        mvc.perform(recordView(artistId).header("X-Visitor-Id", "first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(11));
        mvc.perform(get("/api/artist-profiles/{profileId}", profile.getId()).contextPath("/api")
                        .param("exhibitionId", exhibitionId.toString()).header("X-Visitor-Id", "first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(12));
        mvc.perform(getArtist(artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(12));
    }

    @Test
    @DisplayName("없는 작가, 삭제된 작가, 공통 프로필이 없는 작가는 404를 반환한다")
    void rejectsArtistsWithoutAnActiveProfile() throws Exception {
        ArtistProfile deleted = createProfile(0);
        deleted.getArtist().softDelete(LocalDateTime.now());
        Artist withoutProfile = artists.save(Artist.builder().nameKo("프로필 없는 작가").build());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(recordView(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_404"));
        mvc.perform(recordView(deleted.getArtist().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_404"));
        mvc.perform(recordView(withoutProfile.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_PROFILE_404"));
    }

    @Test
    @DisplayName("같은 방문자의 동시 요청은 한 번만 집계한다")
    void countsConcurrentRequestsOnce() throws Exception {
        ArtistProfile profile = createProfile(0);
        UUID artistId = profile.getArtist().getId();
        // 각 요청은 별도 트랜잭션에서 실행되므로 테스트 데이터를 먼저 커밋한다.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier start = new CyclicBarrier(2);
            Callable<Boolean> request = () -> {
                start.await(5, TimeUnit.SECONDS);
                byte[] body = mvc.perform(recordView(artistId).header("X-Visitor-Id", "concurrent_visitor"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.counted").isBoolean())
                        .andExpect(jsonPath("$.data.viewCount").value(1))
                        .andReturn().getResponse().getContentAsByteArray();
                return objectMapper.readTree(body).get("data").get("counted").asBoolean();
            };

            Future<Boolean> first = executor.submit(request);
            Future<Boolean> second = executor.submit(request);
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1L, profiles.findViewCountById(profile.getId()));
        } finally {
            executor.shutdownNow();
            artists.deleteById(artistId);
        }
    }

    private ArtistProfile createProfile(long viewCount) {
        Artist artist = artists.save(Artist.builder().nameKo("테스트 작가").build());
        ArtistProfile profile = profiles.save(ArtistProfile.builder().artist(artist)
                .nameKo("테스트 작가").viewCount(viewCount).build());
        artist.linkProfile(profile);
        return profile;
    }

    private UUID createExhibition(Artist artist) {
        Account organizer = accounts.save(Account.builder().role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());
        Exhibition exhibition = exhibitions.save(Exhibition.builder().account(organizer)
                .univName("테스트 대학").deptName("테스트 학과").slug("artist-views-" + UUID.randomUUID())
                .isPublic(true).build());
        exhibitionDetails.save(ExhibitionDetail.builder().exhibition(exhibition).title("테스트 전시").build());
        exhibitionArtists.save(ExhibitionArtistMap.builder().artist(artist).exhibition(exhibition)
                .status(ExhibitionArtistStatus.JOINED).build());
        return exhibition.getId();
    }

    private MockHttpServletRequestBuilder recordView(UUID artistId) {
        return post("/api/artists/{artistId}/views", artistId).contextPath("/api");
    }

    private MockHttpServletRequestBuilder getArtist(UUID artistId) {
        return get("/api/artists/{artistId}", artistId).contextPath("/api");
    }
}
