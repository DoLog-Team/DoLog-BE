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
import com.dolog.server.domain.bts.entity.Bts;
import com.dolog.server.domain.bts.entity.BtsArtworkMap;
import com.dolog.server.domain.bts.repository.BtsArtworkMapRepository;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkCommandApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired BtsRepository btsRepository;
    @Autowired BtsArtworkMapRepository btsArtworkMaps;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @MockitoBean FileService fileService;

    private Artist me;
    private String myToken;
    private Artist other;
    private String otherToken;

    @BeforeEach
    void setUp() {
        // 앞 테스트 롤백 시 실행되는 업로드 파일 정리 호출이 남아 있을 수 있어 초기화한다.
        reset(fileService);
        me = artist("나작가");
        myToken = token(me.getAccount());
        other = artist("남작가");
        otherToken = token(other.getAccount());
    }

    // ---------------- 등록 ----------------

    @Test
    @DisplayName("작가 어드민이 작품을 등록하면 본인 작품으로 전시 미소속 DRAFT 상태로 생성된다")
    void createsDraftArtworkForLoginArtist() throws Exception {
        String id = json(mvc.perform(withToken(multipart("/api/artworks"), myToken)
                        .param("title", " 파도의 그릇 ")
                        .param("description", "첫 줄\\n둘째 줄")
                        .param("category", "청화백자")
                        .param("materials", "백자토", " ", "청화안료")
                        .param("width", "32.5")
                        .param("depth", "7")
                        .param("productionStartYear", "2026")
                        .param("productionEndYear", "2026")
                        .param("productionEndMonth", "6")
                        .param("purchaseChatUrl", "https://open.kakao.com/o/test")
                        .param("showPurchaseButton", "true")
                        .param("artistRole", "작가"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CREATED_201"))
                .andExpect(jsonPath("$.data.title").value("파도의 그릇"))
                .andExpect(jsonPath("$.data.description").value("첫 줄\n둘째 줄"))
                .andExpect(jsonPath("$.data.materials", contains("백자토", "청화안료")))
                .andExpect(jsonPath("$.data.width").value(32.5))
                .andExpect(jsonPath("$.data.height").value(nullValue()))
                .andExpect(jsonPath("$.data.productionEndMonth").value(6))
                .andExpect(jsonPath("$.data.showPurchaseButton").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.exhibitionId").value(nullValue()))
                .andExpect(jsonPath("$.data.zoneId").value(nullValue()))
                .andExpect(jsonPath("$.data.mainImage").value(nullValue()))
                .andExpect(jsonPath("$.data.artistName").value("나작가")), "$.data.id");

        em.flush();
        em.clear();
        Artwork saved = artworks.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(ArtworkStatus.DRAFT, saved.getStatus());
        assertNull(saved.getExhibition());
        assertNull(saved.getOrderIndex());
        assertEquals(1, saved.getArtworkArtistMaps().size());
        ArtworkArtistMap map = saved.getArtworkArtistMaps().get(0);
        assertEquals(me.getId(), map.getArtist().getId());
        assertNull(map.getArtistProfile());
        assertEquals("작가", map.getArtistRole());
        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("대표 이미지를 보내면 업로드한 URL 이 저장된다")
    void uploadsMainImage() throws Exception {
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/main.webp");

        mvc.perform(withToken(multipart("/api/artworks").file(image("mainImageFile")), myToken)
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mainImage").value("https://s3/main.webp"));
    }

    @Test
    @DisplayName("제목이나 설명이 없으면 400")
    void rejectsMissingRequiredFields() throws Exception {
        mvc.perform(withToken(multipart("/api/artworks"), myToken).param("description", "설명"))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(multipart("/api/artworks"), myToken).param("title", "작품"))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(multipart("/api/artworks"), myToken)
                        .param("title", "작품").param("description", "설명").param("productionEndMonth", "13"))
                .andExpect(status().isBadRequest());
        assertEquals(0, countArtworksOf(me));
    }

    @Test
    @DisplayName("다른 작가의 artistId 로 등록하면 403")
    void rejectsOtherArtistId() throws Exception {
        mvc.perform(withToken(multipart("/api/artworks"), myToken)
                        .param("artistId", other.getId().toString())
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_007"));
        assertEquals(0, countArtworksOf(other));
    }

    @Test
    @DisplayName("본인 artistId 를 보내면 그대로 등록된다")
    void acceptsOwnArtistId() throws Exception {
        mvc.perform(withToken(multipart("/api/artworks"), myToken)
                        .param("artistId", me.getId().toString())
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("작가 정보를 아직 만들지 않은 작가 어드민은 404")
    void rejectsArtistAdminWithoutArtist() throws Exception {
        Account account = accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());

        mvc.perform(withToken(multipart("/api/artworks"), token(account))
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTIST_404"));
    }

    @Test
    @DisplayName("작가 어드민이 아니면 403, 토큰이 없으면 401")
    void requiresArtistAdmin() throws Exception {
        Account admin = accounts.saveAndFlush(Account.builder().role(Role.DOLOG_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());

        mvc.perform(withToken(multipart("/api/artworks"), token(admin))
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/artworks").contextPath("/api")
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- 수정 ----------------

    @Test
    @DisplayName("본인 작품을 수정하면 보낸 필드만 바뀌고 상태는 그대로다")
    void patchesOnlySentFields() throws Exception {
        Artwork artwork = artworkOf(me, "원래 제목");
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        em.flush();

        mvc.perform(withToken(patch(artwork.getId()), myToken)
                        .param("title", "바뀐 제목")
                        .param("height", "40.25")
                        .param("productionStartMonth", "3")
                        .param("artistRole", "대표 작가"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.data.description").value("원래 설명"))
                .andExpect(jsonPath("$.data.category").value("원래 카테고리"))
                .andExpect(jsonPath("$.data.height").value(40.25))
                .andExpect(jsonPath("$.data.productionStartMonth").value(3))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        em.flush();
        em.clear();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertEquals("바뀐 제목", found.getTitle());
        assertEquals(0, new BigDecimal("40.25").compareTo(found.getHeight()));
        assertEquals(ArtworkStatus.PUBLISHED, found.getStatus());
        assertEquals("대표 작가", found.getArtworkArtistMaps().get(0).getArtistRole());
    }

    @Test
    @DisplayName("재료를 보내면 교체되고, 빈 값 하나만 보내면 모두 지워진다")
    void replacesAndClearsMaterials() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        artwork.replaceMaterials(List.of("백자토"));
        em.flush();

        mvc.perform(withToken(patch(artwork.getId()), myToken).param("materials", "유약", "청화안료"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials", contains("유약", "청화안료")));
        endRequest();
        assertEquals(2L, materialCount(artwork.getId()));
        mvc.perform(withToken(patch(artwork.getId()), myToken).param("materials", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.materials").isEmpty());

        endRequest();
        assertEquals(0L, materialCount(artwork.getId()));
    }

    @Test
    @DisplayName("제목을 공백으로 바꾸려 하면 400")
    void rejectsBlankTitleOnPatch() throws Exception {
        Artwork artwork = artworkOf(me, "작품");

        mvc.perform(withToken(patch(artwork.getId()), myToken).param("title", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 작가의 작품은 수정할 수 없고 존재를 숨기기 위해 404")
    void cannotPatchOthersArtwork() throws Exception {
        Artwork artwork = artworkOf(other, "남의 작품");

        mvc.perform(withToken(patch(artwork.getId()), myToken).param("title", "탈취"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));

        em.flush();
        em.clear();
        assertEquals("남의 작품", artworks.findById(artwork.getId()).orElseThrow().getTitle());
    }

    @Test
    @DisplayName("선택 문자열 필드에 빈 값을 보내면 빈 문자열이 아니라 null 로 비워진다")
    void blankOptionalStringsClearToNull() throws Exception {
        Artwork artwork = artworks.saveAndFlush(linked(Artwork.builder().title("작품").description("설명")
                .category("분류").shortIntro("소개").purchaseUrl("https://buy")
                .purchaseChatUrl("https://chat").youtubeUrl("https://yt").build(), me));

        mvc.perform(withToken(patch(artwork.getId()), myToken)
                        .param("category", "").param("shortIntro", " ").param("purchaseUrl", "")
                        .param("purchaseChatUrl", "").param("youtubeUrl", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value(nullValue()))
                .andExpect(jsonPath("$.data.shortIntro").value(nullValue()));

        endRequest();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertNull(found.getCategory());
        assertNull(found.getShortIntro());
        assertNull(found.getPurchaseUrl());
        assertNull(found.getPurchaseChatUrl());
        assertNull(found.getYoutubeUrl());
    }

    @Test
    @DisplayName("등록 시 선택 문자열이 빈 값이면 null 로 저장된다")
    void blankOptionalStringsAreNullOnCreate() throws Exception {
        mvc.perform(withToken(multipart("/api/artworks"), myToken)
                        .param("title", "작품").param("description", "설명")
                        .param("category", "").param("purchaseUrl", " "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value(nullValue()))
                .andExpect(jsonPath("$.data.purchaseUrl").value(nullValue()));
    }

    @Test
    @DisplayName("[현재 정책] 공동 작가도 작품을 삭제할 수 있다 — 대표 작가만 허용할지 확인 필요")
    void coArtistCanDeleteForNow() throws Exception {
        Artwork artwork = artworkOf(other, "공동 작품");
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(me).artistRole("공동 작가").build());
        artworks.saveAndFlush(artwork);

        mvc.perform(withToken(delete("/api/artworks/{id}", artwork.getId()).contextPath("/api"), myToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이미지 업로드가 실패하면 ARTWORK_004, 허용되지 않는 형식이면 400 ARTWORK_008")
    void uploadErrorsUseArtworkCodes() throws Exception {
        when(fileService.uploadFile(any(), any())).thenThrow(new java.io.IOException("io"));
        mvc.perform(withToken(multipart("/api/artworks").file(image("mainImageFile")), myToken)
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ARTWORK_004"));

        reset(fileService);
        when(fileService.uploadFile(any(), any())).thenThrow(new IllegalArgumentException("형식"));
        mvc.perform(withToken(multipart("/api/artworks").file(image("mainImageFile")), myToken)
                        .param("title", "작품").param("description", "설명"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_008"));
        assertEquals(0, countArtworksOf(me));
    }

    @Test
    @DisplayName("대표 이미지를 바꾸면 새 파일을 올리고, 기존 파일은 커밋 전에는 지우지 않는다")
    void replacesMainImage() throws Exception {
        Artwork artwork = artworks.saveAndFlush(linked(Artwork.builder()
                .title("작품").description("설명").mainImg("https://s3/old.webp").build(), me));
        when(fileService.uploadFile(any(), eq("artworks/main"))).thenReturn("https://s3/new.webp");

        mvc.perform(withToken(patch(artwork.getId()).file(image("mainImageFile")), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainImage").value("https://s3/new.webp"));
        // 테스트 트랜잭션은 커밋되지 않으므로 삭제가 일어나면 안 된다. 커밋 후 삭제는 ArtworkFileHandlerTest 에서 확인
        verify(fileService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("이미지를 안 보내면 기존 대표 이미지를 지우지 않는다")
    void keepsMainImageWhenNotSent() throws Exception {
        Artwork artwork = artworks.saveAndFlush(linked(Artwork.builder()
                .title("작품").description("설명").mainImg("https://s3/old.webp").build(), me));

        mvc.perform(withToken(patch(artwork.getId()), myToken).param("title", "새 제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainImage").value("https://s3/old.webp"));
        verify(fileService, never()).deleteFile(any());
    }

    // ---------------- 삭제 ----------------

    @Test
    @DisplayName("본인 작품을 삭제하면 숨겨지고, 재료는 보관 기간 동안 남으며 BTS 연결은 조회되지 않는다")
    void deletesOwnArtworkWithRelations() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        artwork.replaceMaterials(List.of("백자토"));
        Exhibition exhibition = exhibition();
        Bts bts = btsRepository.saveAndFlush(Bts.builder().exhibition(exhibition).title("비하인드").build());
        btsArtworkMaps.saveAndFlush(BtsArtworkMap.builder().bts(bts).artwork(artwork).build());
        UUID id = artwork.getId();

        mvc.perform(withToken(delete("/api/artworks/{id}", id).contextPath("/api"), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        em.flush();
        em.clear();
        assertTrue(artworks.findById(id).isEmpty());
        // 소프트 삭제라 재료는 WithdrawalRetentionJob 이 보관 기간 후 정리한다.
        assertEquals(1L, materialCount(id));
        assertEquals(0L, em.createQuery("SELECT COUNT(m) FROM BtsArtworkMap m WHERE m.artwork.id = :id", Long.class)
                .setParameter("id", id).getSingleResult());
        assertTrue(btsRepository.findById(bts.getId()).isPresent());
    }

    @Test
    @DisplayName("다른 작가의 작품은 삭제할 수 없고 404")
    void cannotDeleteOthersArtwork() throws Exception {
        Artwork artwork = artworkOf(other, "남의 작품");

        mvc.perform(withToken(delete("/api/artworks/{id}", artwork.getId()).contextPath("/api"), myToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));

        em.flush();
        em.clear();
        assertTrue(artworks.findById(artwork.getId()).isPresent());
    }

    @Test
    @DisplayName("없는 작품을 삭제하면 404")
    void deleteMissingArtwork() throws Exception {
        mvc.perform(withToken(delete("/api/artworks/{id}", UUID.randomUUID()).contextPath("/api"), otherToken))
                .andExpect(status().isNotFound());
    }

    // ---------------- helpers ----------------

    private Artist artist(String name) {
        Account account = accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());
        return artists.saveAndFlush(Artist.builder().account(account).nameKo(name).build());
    }

    private String token(Account account) {
        RefreshToken session = tokens.saveAndFlush(RefreshToken.builder().account(account)
                .token(jwt.createRefreshToken(account.getId())).build());
        return jwt.createAccessToken(account.getId(), session.getId());
    }

    private Exhibition exhibition() {
        Account account = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());
        return exhibitions.saveAndFlush(Exhibition.builder().account(account).univName("테스트 대학")
                .deptName("테스트 학과").slug("artwork-api-" + UUID.randomUUID()).build());
    }

    private Artwork artworkOf(Artist artist, String title) {
        return artworks.saveAndFlush(linked(Artwork.builder().title(title)
                .description("원래 설명").category("원래 카테고리").build(), artist));
    }

    private Artwork linked(Artwork artwork, Artist artist) {
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artwork;
    }

    private MockMultipartHttpServletRequestBuilder patch(UUID artworkId) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/artworks/{id}", artworkId);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });
        return builder;
    }

    private <T extends org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> T withToken(
            T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, "main.png", "image/png", new byte[]{1, 2, 3});
    }

    private long countArtworksOf(Artist artist) {
        em.flush();
        return em.createQuery("SELECT COUNT(m) FROM ArtworkArtistMap m WHERE m.artist.id = :id", Long.class)
                .setParameter("id", artist.getId()).getSingleResult();
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }

    private long materialCount(UUID artworkId) {
        return em.createQuery("SELECT COUNT(m) FROM ArtworkMaterial m WHERE m.artwork.id = :id", Long.class)
                .setParameter("id", artworkId).getSingleResult();
    }

    private String json(org.springframework.test.web.servlet.ResultActions result, String path) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
