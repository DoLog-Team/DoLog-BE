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
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.repository.ArtworkImgRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.global.jwt.JwtTokenProvider;
import com.dolog.server.global.util.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkImageApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired ArtworkImgRepository images;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @MockitoBean FileService fileService;

    private Artist me;
    private String myToken;
    private Artwork artwork;

    @BeforeEach
    void setUp() throws Exception {
        reset(fileService);
        when(fileService.uploadFile(any(), eq("artworks/detail")))
                .thenReturn("https://s3/detail-1.webp", "https://s3/detail-2.webp", "https://s3/detail-3.webp");
        me = artist("나작가");
        myToken = token(me.getAccount());
        artwork = artworkOf(me);
    }

    @Test
    @DisplayName("작가 본인은 상세 이미지를 등록하고 201, 순서를 안 보내면 기존 이미지 뒤에 붙는다")
    void ownerCreatesImages() throws Exception {
        imageOf(artwork, "https://s3/old.webp", 5);
        endRequest();

        mvc.perform(withToken(create(artwork.getId())
                        .file(image("images[0].imageFile"))
                        .file(image("images[1].imageFile"))
                        .param("images[0].description", "앞면"), myToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpStatus").value(201));
        endRequest();

        List<ArtworkImg> saved = images.findAll().stream()
                .filter(img -> img.getArtwork().getId().equals(artwork.getId()))
                .sorted(Comparator.comparing(ArtworkImg::getOrderIndex))
                .toList();
        assertEquals(3, saved.size());
        assertEquals(List.of(5, 6, 7), saved.stream().map(ArtworkImg::getOrderIndex).toList());
        assertEquals("앞면", saved.get(1).getDescription());
        verify(fileService, times(2)).uploadFile(any(), eq("artworks/detail"));
    }

    @Test
    @DisplayName("남의 작품이면 404, 전시 어드민은 403, 비로그인은 401 이고 업로드하지 않는다")
    void rejectsOthers() throws Exception {
        Artist other = artist("남작가");

        mvc.perform(withToken(create(artwork.getId()).file(image("images[0].imageFile")), token(other.getAccount())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        mvc.perform(withToken(create(artwork.getId()).file(image("images[0].imageFile")), exhibitionAdminToken()))
                .andExpect(status().isForbidden());
        mvc.perform(create(artwork.getId()).file(image("images[0].imageFile")).contextPath("/api"))
                .andExpect(status().isUnauthorized());

        verify(fileService, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("두록 어드민은 아무 작품에나 등록할 수 있다")
    void dologAdminCreatesImages() throws Exception {
        mvc.perform(withToken(create(artwork.getId()).file(image("images[0].imageFile")), token(account(Role.DOLOG_ADMIN))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이미지 파일이 없으면 400 ARTWORK_019")
    void requiresImageFile() throws Exception {
        mvc.perform(withToken(create(artwork.getId()), myToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_019"));
        mvc.perform(withToken(create(artwork.getId()).param("images[0].description", "파일 없음"), myToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파일을 바꾸면 새 파일로 저장하고 이전 파일은 커밋된 뒤에 지운다")
    void ownerReplacesImageFile() throws Exception {
        ArtworkImg img = imageOf(artwork, "https://s3/old.webp", 1);
        endRequest();

        mvc.perform(withToken(update(artwork.getId(), img.getId())
                        .file(image("imageFile"))
                        .param("description", "새 설명")
                        .param("orderIndex", "3"), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.image_id").value(img.getId().toString()));

        verify(fileService, never()).deleteFile(any());
        runAfterCommit();
        verify(fileService).deleteFile("https://s3/old.webp");
        endRequest();

        ArtworkImg found = images.findById(img.getId()).orElseThrow();
        assertEquals("https://s3/detail-1.webp", found.getImageUrl());
        assertEquals("새 설명", found.getDescription());
        assertEquals(3, found.getOrderIndex());
    }

    @Test
    @DisplayName("설명만 바꾸면 파일은 그대로 두고 아무것도 지우지 않는다. 남의 작품 이미지는 404, 다른 작품의 이미지는 400")
    void updatesDescriptionOnlyAndRejectsOthers() throws Exception {
        ArtworkImg img = imageOf(artwork, "https://s3/old.webp", 1);
        Artwork otherArtworkOfMine = artworkOf(me);
        endRequest();

        mvc.perform(withToken(update(artwork.getId(), img.getId()).param("description", "설명만"), myToken))
                .andExpect(status().isOk());
        runAfterCommit();
        verify(fileService, never()).deleteFile(any());
        verify(fileService, never()).uploadFile(any(), any());

        mvc.perform(withToken(update(artwork.getId(), img.getId()).param("description", "x"),
                        token(artist("남작가").getAccount())))
                .andExpect(status().isNotFound());
        mvc.perform(withToken(update(otherArtworkOfMine.getId(), img.getId()).param("description", "x"), myToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_003"));
        mvc.perform(withToken(update(artwork.getId(), UUID.randomUUID()).param("description", "x"), myToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_002"));
    }

    @Test
    @DisplayName("작가 본인이 삭제하면 행이 지워지고 파일은 커밋된 뒤에 지운다. 남의 작품이면 404")
    void ownerDeletesImage() throws Exception {
        ArtworkImg img = imageOf(artwork, "https://s3/old.webp", 1);
        endRequest();

        mvc.perform(withToken(delete("/api/artworks/{a}/images/{i}", artwork.getId(), img.getId()),
                        token(artist("남작가").getAccount())))
                .andExpect(status().isNotFound());

        mvc.perform(withToken(delete("/api/artworks/{a}/images/{i}", artwork.getId(), img.getId()), myToken))
                .andExpect(status().isOk());
        verify(fileService, never()).deleteFile(any());
        runAfterCommit();
        verify(fileService).deleteFile("https://s3/old.webp");
        endRequest();

        assertTrue(images.findById(img.getId()).isEmpty());
    }

    @Test
    @DisplayName("전시 어드민은 본인 전시만 재정렬하고 남의 전시는 403, 두록 어드민은 전부, 작가는 403")
    void reorderPermissions() throws Exception {
        Account owner = account(Role.EXHIBITION_ADMIN);
        Exhibition exhibition = exhibition(owner);
        ExhibitionZone zone = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("1구역").orderId(1).build());
        Artwork b = submitted(artworkOf(me, "나"), exhibition, zone, 10);
        Artwork a = submitted(artworkOf(me, "가"), exhibition, zone, 20);
        endRequest();

        mvc.perform(withToken(put("/api/exhibitions/{id}/artworks/reorder", exhibition.getId()), token(owner)))
                .andExpect(status().isOk());
        endRequest();
        assertEquals(10, artworks.findById(a.getId()).orElseThrow().getOrderIndex());
        assertEquals(20, artworks.findById(b.getId()).orElseThrow().getOrderIndex());

        mvc.perform(withToken(put("/api/exhibitions/{id}/artworks/reorder", exhibition.getId()), exhibitionAdminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXHIBITION_403_2"));
        mvc.perform(withToken(put("/api/exhibitions/{id}/artworks/reorder", exhibition.getId()), myToken))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(put("/api/exhibitions/{id}/artworks/reorder", exhibition.getId()), token(account(Role.DOLOG_ADMIN))))
                .andExpect(status().isOk());
        mvc.perform(withToken(put("/api/exhibitions/{id}/artworks/reorder", UUID.randomUUID()), token(owner)))
                .andExpect(status().isNotFound());
    }

    // 테스트는 한 트랜잭션이라 커밋이 없다. 등록된 커밋 후 작업을 직접 실행해 확인한다.
    private void runAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
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

    // 전시가 없는 전시 어드민 계정은 인증 단계에서 막히므로 전시를 하나 붙여 둔다.
    private String exhibitionAdminToken() {
        Account admin = account(Role.EXHIBITION_ADMIN);
        exhibition(admin);
        return token(admin);
    }

    private Exhibition exhibition(Account owner) {
        return exhibitions.saveAndFlush(Exhibition.builder().account(owner).univName("테스트 대학")
                .deptName("테스트 학과").slug("image-" + UUID.randomUUID()).isPublic(true).build());
    }

    private Artwork artworkOf(Artist artist) {
        return artworkOf(artist, "작품");
    }

    private Artwork artworkOf(Artist artist, String title) {
        Artwork artwork = Artwork.builder().title(title).description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private Artwork submitted(Artwork artwork, Exhibition exhibition, ExhibitionZone zone, int orderIndex) {
        artwork.submitTo(exhibition, zone);
        artwork.updateOrder(orderIndex);
        return artworks.saveAndFlush(artwork);
    }

    private ArtworkImg imageOf(Artwork artwork, String url, int orderIndex) {
        return images.saveAndFlush(ArtworkImg.builder().artwork(artwork).imageUrl(url).orderIndex(orderIndex).build());
    }

    private MockMultipartHttpServletRequestBuilder create(UUID artworkId) {
        return multipart("/api/artworks/{id}/images", artworkId);
    }

    private MockMultipartHttpServletRequestBuilder update(UUID artworkId, UUID imageId) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/artworks/{a}/images/{i}", artworkId, imageId);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });
        return builder;
    }

    private <T extends MockHttpServletRequestBuilder> T withToken(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, "detail.png", "image/png", new byte[]{1, 2, 3});
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
