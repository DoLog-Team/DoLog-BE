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
import com.dolog.server.global.util.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class ArtistProfileClearApiTest {

    private static final String PROFILE_EMAIL = "profile@test.com";
    private static final String OLD_IMAGE = "https://cdn.test/old.webp";
    private static final String NEW_IMAGE = "https://cdn.test/new.webp";

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired EntityManager entityManager;
    @MockitoBean FileService files;

    private Account owner;
    private String accountEmail;
    private UUID artistId;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        accountEmail = "account-" + UUID.randomUUID() + "@test.com";
        owner = accounts.saveAndFlush(Account.builder()
                .email(accountEmail)
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artists.saveAndFlush(Artist.builder().account(owner)
                .nameKo("기본이름").nameEn("Basic English").phone("01012345678").build());
        ArtistProfile profile = profiles.saveAndFlush(ArtistProfile.builder().artist(artist)
                .nameKo("프로필이름").nameEn("Profile English").bio("기존 소개")
                .email(PROFILE_EMAIL).profileImg(OLD_IMAGE)
                .purchaseContactUrl("https://example.com/contact").build());
        artist.linkProfile(profile);
        artistId = artist.getId();
        profileId = profile.getId();
    }

    @ParameterizedTest(name = "byId={0}, request={1}")
    @MethodSource("emailUpdates")
    void updatesOnlyProfileEmail(boolean byId, String json, String expectedEmail, int expectedStatus)
            throws Exception {
        mvc.perform(update(byId, json).with(user(details(owner))))
                .andExpect(status().is(expectedStatus));

        assertPersistedState(expectedEmail, OLD_IMAGE);
        verifyNoInteractions(files);
    }

    @ParameterizedTest(name = "byId={0}, request={1}, file={2}")
    @MethodSource("imageUpdates")
    void deletesOrReplacesOnlyProfileImage(
            boolean byId, String json, String fileKind, String expectedImage, int expectedStatus
    ) throws Exception {
        MockMultipartHttpServletRequestBuilder request = update(byId, json);
        if ("empty".equals(fileKind)) {
            request.file(new MockMultipartFile("profileImg", "", MediaType.IMAGE_PNG_VALUE, new byte[0]));
        } else if ("new".equals(fileKind)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
            request.file(new MockMultipartFile("profileImg", "profile.png", MediaType.IMAGE_PNG_VALUE,
                    bytes.toByteArray()));
            if (expectedStatus == 200) {
                when(files.uploadFile(any(), eq("artist-profiles"))).thenReturn(NEW_IMAGE);
            }
        }

        mvc.perform(request.with(user(details(owner)))).andExpect(status().is(expectedStatus));

        assertPersistedState(PROFILE_EMAIL, expectedImage);
        if (expectedStatus == 400 || OLD_IMAGE.equals(expectedImage)) {
            verifyNoInteractions(files);
        } else {
            verify(files).deleteFile(OLD_IMAGE);
            if (NEW_IMAGE.equals(expectedImage)) {
                verify(files).uploadFile(any(), eq("artist-profiles"));
            } else {
                verify(files, never()).uploadFile(any(), any());
                // 이미 삭제된 이미지에 대한 재요청도 성공한다.
                mvc.perform(update(byId, "{\"deleteProfileImg\":true}").with(user(details(owner))))
                        .andExpect(status().isOk());
                assertPersistedState(PROFILE_EMAIL, null);
            }
        }
    }

    @Test
    void clearsProfileEmailAndImageTogetherWithoutChangingAccount() throws Exception {
        mvc.perform(update(false, "{\"email\":\"\",\"deleteProfileImg\":true}")
                        .with(user(details(owner))))
                .andExpect(status().isOk());

        assertPersistedState(null, null);
        verify(files).deleteFile(OLD_IMAGE);
        verify(files, never()).uploadFile(any(), any());
    }

    @Test
    void anotherArtistCannotClearEmailOrImage() throws Exception {
        Account other = accounts.saveAndFlush(Account.builder().email("other-" + UUID.randomUUID() + "@test.com")
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        mvc.perform(update(true, "{\"email\":\"\",\"deleteProfileImg\":true}")
                        .with(user(details(other))))
                .andExpect(status().isForbidden());

        assertPersistedState(PROFILE_EMAIL, OLD_IMAGE);
        verifyNoInteractions(files);
    }

    private void assertPersistedState(String expectedEmail, String expectedImage) throws Exception {
        entityManager.flush();
        entityManager.clear();
        Artist artist = artists.findById(artistId).orElseThrow();
        ArtistProfile profile = profiles.findById(profileId).orElseThrow();
        assertEquals(accountEmail, accounts.findById(owner.getId()).orElseThrow().getEmail());
        assertEquals(expectedEmail, profile.getEmail());
        assertEquals(expectedImage, profile.getProfileImg());
        assertEquals("기본이름", artist.getNameKo());
        assertEquals("Basic English", artist.getNameEn());
        assertEquals("01012345678", artist.getPhone());
        assertEquals("프로필이름", profile.getNameKo());
        assertEquals("Profile English", profile.getNameEn());
        assertEquals("기존 소개", profile.getBio());
        assertEquals("https://example.com/contact", profile.getPurchaseContactUrl());
        for (String path : List.of("/api/artists/" + artistId, "/api/artist-profiles/me")) {
            mvc.perform(get(path).contextPath("/api").with(user(details(owner))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(expectedEmail == null ? nullValue() : equalTo(expectedEmail)))
                    .andExpect(jsonPath("$.data.profileImg").value(expectedImage == null ? nullValue() : equalTo(expectedImage)));
        }
    }

    private MockMultipartHttpServletRequestBuilder update(boolean byId, String json) {
        var request = multipart(HttpMethod.PATCH, "/api/artist-profiles/" + (byId ? profileId : "me"));
        request.file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8)));
        request.contextPath("/api");
        return request;
    }

    private CustomUserDetails details(Account account) {
        return new CustomUserDetails(account.getId(), 1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_ARTIST_ADMIN")));
    }

    static Stream<Arguments> emailUpdates() {
        return Stream.of(false, true).flatMap(byId -> Stream.of(
                Arguments.of(byId, "{\"email\":\"\"}", null, 200),
                Arguments.of(byId, "{}", PROFILE_EMAIL, 200),
                Arguments.of(byId, "{\"email\":null}", PROFILE_EMAIL, 200),
                Arguments.of(byId, "{\"email\":\"new@test.com\"}", "new@test.com", 200),
                Arguments.of(byId, "{\"email\":\"invalid\"}", PROFILE_EMAIL, 400),
                Arguments.of(byId, "{\"email\":\"   \"}", PROFILE_EMAIL, 400)
        ));
    }

    static Stream<Arguments> imageUpdates() {
        return Stream.of(false, true).flatMap(byId -> Stream.of(
                Arguments.of(byId, "{\"deleteProfileImg\":true}", "none", null, 200),
                Arguments.of(byId, "{\"deleteProfileImg\":true}", "empty", null, 200),
                Arguments.of(byId, "{\"deleteProfileImg\":false}", "none", OLD_IMAGE, 200),
                Arguments.of(byId, "{\"deleteProfileImg\":null}", "none", OLD_IMAGE, 200),
                Arguments.of(byId, "{}", "none", OLD_IMAGE, 200),
                Arguments.of(byId, "{}", "empty", null, 200),
                Arguments.of(byId, "{}", "new", NEW_IMAGE, 200),
                Arguments.of(byId, "{\"email\":\"\",\"deleteProfileImg\":true}", "new", OLD_IMAGE, 400)
        ));
    }
}
