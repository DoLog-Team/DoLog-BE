package com.dolog.server.domain.artist.web.controller;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.global.security.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class ArtistNameUpdateApiTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired EntityManager entityManager;

    @ParameterizedTest(name = "profileApi={0}, request={1}")
    @MethodSource("nameUpdates")
    void updatesOnlyKoreanNameAndReturnsPersistedName(
            boolean profileApi, String json, String expectedName, int expectedStatus
    ) throws Exception {
        Account owner = accounts.saveAndFlush(Account.builder()
                .email("name-" + UUID.randomUUID() + "@test.com")
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artists.saveAndFlush(Artist.builder().account(owner)
                .nameKo("기존이름").nameEn("Basic English").phone("01012345678").build());
        ArtistProfile profile = profiles.saveAndFlush(ArtistProfile.builder().artist(artist)
                .nameKo("기존이름").nameEn("Profile English").bio("기존 소개")
                .email("profile@test.com").profileImg("https://cdn.test/profile.webp")
                .purchaseContactUrl("https://example.com/contact").build());
        artist.linkProfile(profile);
        UUID artistId = artist.getId();
        UUID profileId = profile.getId();
        CustomUserDetails details = new CustomUserDetails(owner.getId(), 1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_ARTIST_ADMIN")));

        MockHttpServletRequestBuilder update = profileApi
                ? multipart(HttpMethod.PATCH, "/api/artist-profiles/me")
                        .file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                                json.getBytes(StandardCharsets.UTF_8)))
                : patch("/api/artists/me").contentType(MediaType.APPLICATION_JSON).content(json);
        mvc.perform(update.contextPath("/api").with(user(details)))
                .andExpect(status().is(expectedStatus));

        entityManager.flush();
        entityManager.clear();

        Artist persistedArtist = artists.findById(artistId).orElseThrow();
        ArtistProfile persistedProfile = profiles.findById(profileId).orElseThrow();
        assertEquals(profileApi ? "기존이름" : expectedName, persistedArtist.getNameKo());
        assertEquals(expectedName, persistedProfile.getNameKo());
        assertEquals("Basic English", persistedArtist.getNameEn());
        assertEquals("01012345678", persistedArtist.getPhone());
        assertEquals("Profile English", persistedProfile.getNameEn());
        assertEquals("기존 소개", persistedProfile.getBio());
        assertEquals("profile@test.com", persistedProfile.getEmail());
        assertEquals("https://cdn.test/profile.webp", persistedProfile.getProfileImg());
        assertEquals("https://example.com/contact", persistedProfile.getPurchaseContactUrl());

        mvc.perform(get("/api/artists/{artistId}", artistId).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameKo").value(expectedName == null ? nullValue() : equalTo(expectedName)));
        mvc.perform(get("/api/artist-profiles/me").contextPath("/api").with(user(details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameKo").value(expectedName == null ? nullValue() : equalTo(expectedName)));
    }

    static Stream<Arguments> nameUpdates() {
        return Stream.of(false, true).flatMap(profileApi -> Stream.of(
                Arguments.of(profileApi, "{\"nameKo\":\"\"}", null, 200),
                Arguments.of(profileApi, "{}", "기존이름", 200),
                Arguments.of(profileApi, "{\"nameKo\":null}", "기존이름", 200),
                Arguments.of(profileApi, "{\"nameKo\":\" 수정이름 \"}", "수정이름", 200),
                Arguments.of(profileApi, "{\"nameKo\":\"   \"}", "기존이름", 400)
        ));
    }
}
