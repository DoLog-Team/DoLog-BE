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
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkImgRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@AutoConfigureMockMvc
class ArtworkFullUpdateApiTests {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtworkRepository artworks;
    @Autowired ArtworkImgRepository images;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired ExhibitionArtistMapRepository participations;
    @Autowired ExhibitionFieldSettingsRepository fieldSettings;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @MockitoBean FileService fileService;

    private String adminToken;
    private Exhibition exhibition;
    private ExhibitionZone zone;
    private Artist me;
    private ArtistProfile myProfile;
    private Artist co;
    private ArtistProfile coProfile;
    private Artwork artwork;

    @BeforeEach
    void setUp() throws Exception {
        reset(fileService);
        when(fileService.uploadFile(any(), eq("artworks/detail"))).thenReturn("https://s3/new-detail.webp");
        adminToken = token(account(Role.DOLOG_ADMIN));
        exhibition = exhibition();
        zone = zone(exhibition, "1구역");
        me = artist("나작가");
        myProfile = join(me, exhibition, ExhibitionArtistStatus.JOINED);
        co = artist("공동작가");
        coProfile = join(co, exhibition, ExhibitionArtistStatus.JOINED);
        artwork = submittedArtwork(me, myProfile, zone, 10);
        endRequest();
    }

    @Test
    @DisplayName("V2 필드와 공동 작가(프로필 기준)를 한 번에 수정하고, 응답에 프로필 ID 와 상태가 담긴다")
    void updatesFieldsAndArtists() throws Exception {
        mvc.perform(full(artwork)
                        .param("title", " 새 제목 ")
                        .param("description", "새 설명")
                        .param("category", "조소")
                        .param("materials", "브론즈", "나무")
                        .param("width", "30.5")
                        .param("productionEndYear", "2026")
                        .param("purchaseChatUrl", "https://open.kakao.com/o/x")
                        .param("zoneId", zone.getId().toString())
                        .param("artistProfileIds", myProfile.getId().toString(), coProfile.getId().toString())
                        .param("artistRoles[" + coProfile.getId() + "]", "공동 제작"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.updatedArtistIds",
                        containsInAnyOrder(myProfile.getId().toString(), coProfile.getId().toString())))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.hidden").value(false));
        endRequest();

        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertEquals("조소", found.getCategory());
        assertEquals(List.of("브론즈", "나무"), found.getMaterials().stream().map(m -> m.getName()).toList());
        assertEquals(0, new BigDecimal("30.5").compareTo(found.getWidth()));
        assertEquals("https://open.kakao.com/o/x", found.getPurchaseChatUrl());
        Map<UUID, ArtworkArtistMap> maps = found.getArtworkArtistMaps().stream()
                .collect(Collectors.toMap(map -> map.getArtist().getId(), map -> map));
        assertEquals("원래 역할", maps.get(me.getId()).getArtistRole());
        assertEquals("공동 제작", maps.get(co.getId()).getArtistRole());
        assertEquals(coProfile.getId(), maps.get(co.getId()).getArtistProfile().getId());
    }

    @Test
    @DisplayName("목록에서 빠진 작가는 연결이 해제된다")
    void removesMissingArtists() throws Exception {
        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", coProfile.getId().toString()))
                .andExpect(status().isOk());
        endRequest();

        List<ArtworkArtistMap> maps = artworks.findById(artwork.getId()).orElseThrow().getArtworkArtistMaps();
        assertEquals(List.of(co.getId()), maps.stream().map(map -> map.getArtist().getId()).toList());
    }

    @Test
    @DisplayName("다른 전시의 프로필, 참여 중이 아닌 새 작가는 400. 이미 연결된 작가는 제외됐어도 남길 수 있다")
    void validatesArtistProfiles() throws Exception {
        Exhibition other = exhibition();
        ArtistProfile otherExhibitionProfile = join(artist("다른전시"), other, ExhibitionArtistStatus.JOINED);
        ArtistProfile removedProfile = join(artist("제외작가"), exhibition, ExhibitionArtistStatus.REMOVED);
        endRequest();

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", otherExhibitionProfile.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_016"));
        mvc.perform(full(artwork).params(basic())
                        .param("artistProfileIds", myProfile.getId().toString(), removedProfile.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_016"));

        participations.findByExhibitionIdAndArtistId(exhibition.getId(), me.getId()).orElseThrow()
                .updateStatus(ExhibitionArtistStatus.REMOVED);
        endRequest();
        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("path 의 전시에 출품된 작품이 아니면 404, 다른 전시의 구역이면 400")
    void scopesToExhibition() throws Exception {
        Exhibition other = exhibition();
        ExhibitionZone otherZone = zone(other, "다른 구역");
        endRequest();

        mvc.perform(withAdmin(multipart("/api/exhibitions/{e}/artworks/{a}", other.getId(), artwork.getId()))
                        .params(basic()).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));

        var params = basic();
        params.set("zoneId", otherZone.getId().toString());
        mvc.perform(full(artwork).params(params).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_012"));
    }

    @Test
    @DisplayName("구역을 옮기면서 순서를 안 주면 새 구역의 마지막으로 간다. 순서는 앞/뒤를 같이 보내야 한다")
    void movesZoneToLast() throws Exception {
        ExhibitionZone zone2 = zone(exhibition, "2구역");
        Artwork existing = submittedArtwork(me, myProfile, zone2, 50);
        endRequest();

        var params = basic();
        params.set("zoneId", zone2.getId().toString());
        mvc.perform(full(artwork).params(params).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isOk());
        endRequest();

        Artwork moved = artworks.findById(artwork.getId()).orElseThrow();
        assertEquals(zone2.getId(), moved.getExhibitionZone().getId());
        assertTrue(moved.getOrderIndex() > artworks.findById(existing.getId()).orElseThrow().getOrderIndex());

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("prevOrder", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_005"));
    }

    @Test
    @DisplayName("공개로 바꿀 때는 필수 항목을 검사하고, 숨김/재공개를 같이 처리한다")
    void handlesStatusAndHidden() throws Exception {
        ExhibitionFieldSettings settings = ExhibitionFieldSettings.defaultsFor(exhibition);
        settings.update(true, false, false, false, false, false, false, false, false);
        fieldSettings.saveAndFlush(settings);
        endRequest();

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("status", "PUBLISHED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_013"));

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("status", "DRAFT").param("hidden", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.hidden").value(true));
        endRequest();

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("hidden", "false"))
                .andExpect(jsonPath("$.data.hidden").value(false));
    }

    @Test
    @DisplayName("images 를 안 보내면 그대로, 보내면 그 목록으로 맞추고 빠진 이미지 파일은 커밋 뒤에 지운다")
    void syncsImages() throws Exception {
        ArtworkImg keep = images.saveAndFlush(ArtworkImg.builder().artwork(artwork).imageUrl("https://s3/keep.webp").orderIndex(1).build());
        ArtworkImg drop = images.saveAndFlush(ArtworkImg.builder().artwork(artwork).imageUrl("https://s3/drop.webp").orderIndex(2).build());
        endRequest();

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(jsonPath("$.data.updatedImageIds",
                        containsInAnyOrder(keep.getId().toString(), drop.getId().toString())));
        endRequest();

        mvc.perform(full(artwork)
                        .file(new MockMultipartFile("images[1].imageFile", "new.png", "image/png", new byte[]{1}))
                        .params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("images[0].id", keep.getId().toString())
                        .param("images[0].orderIndex", "1")
                        .param("images[1].orderIndex", "2"))
                .andExpect(status().isOk());

        verify(fileService, never()).deleteFile(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        verify(fileService).deleteFile("https://s3/drop.webp");
        endRequest();

        List<String> urls = artworks.findById(artwork.getId()).orElseThrow().getArtworkImg().stream()
                .map(ArtworkImg::getImageUrl).toList();
        assertEquals(List.of("https://s3/keep.webp", "https://s3/new-detail.webp"), urls);
    }

    @Test
    @DisplayName("새 이미지는 파일이 있어야 하고(URL 만 보내면 400), 순서가 없으면 끝에 붙는다")
    void newImageNeedsFileAndAppendsOrder() throws Exception {
        ArtworkImg keep = images.saveAndFlush(ArtworkImg.builder().artwork(artwork).imageUrl("https://s3/keep.webp").orderIndex(3).build());
        endRequest();

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("images[0].id", keep.getId().toString())
                        .param("images[1].imageUrl", "https://evil.example.com/x.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_019"));
        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("images[0].id", keep.getId().toString())
                        .param("images[1].description", "파일 없음"))
                .andExpect(status().isBadRequest());

        mvc.perform(full(artwork)
                        .file(new MockMultipartFile("images[1].imageFile", "new.png", "image/png", new byte[]{1}))
                        .params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("images[0].id", keep.getId().toString()))
                .andExpect(status().isOk());
        endRequest();

        List<Integer> orders = artworks.findById(artwork.getId()).orElseThrow().getArtworkImg().stream()
                .map(ArtworkImg::getOrderIndex).sorted().toList();
        assertEquals(List.of(3, 4), orders);
    }

    @Test
    @DisplayName("작가 역할은 앞뒤 공백을 지우고, 100자를 넘으면 400")
    void trimsAndLimitsRoles() throws Exception {
        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("artistRoles[" + myProfile.getId() + "]", "  조각  "))
                .andExpect(status().isOk());
        endRequest();
        assertEquals("조각", artworks.findById(artwork.getId()).orElseThrow().getArtworkArtistMaps().get(0).getArtistRole());

        mvc.perform(full(artwork).params(basic()).param("artistProfileIds", myProfile.getId().toString())
                        .param("artistRoles[" + myProfile.getId() + "]", "가".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("두록 어드민만 쓸 수 있고, 필수 값이 없으면 400")
    void requiresDologAdminAndRequiredFields() throws Exception {
        mvc.perform(multipart("/api/exhibitions/{e}/artworks/{a}", exhibition.getId(), artwork.getId())
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contextPath("/api").header("Authorization", "Bearer " + token(me.getAccount()))
                        .params(basic()).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isForbidden());

        var params = basic();
        params.remove("title");
        mvc.perform(full(artwork).params(params).param("artistProfileIds", myProfile.getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(full(artwork).params(basic()))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.util.LinkedMultiValueMap<String, String> basic() {
        var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
        params.add("title", "작품");
        params.add("description", "설명");
        params.add("category", "회화");
        params.add("zoneId", zone.getId().toString());
        return params;
    }

    private MockMultipartHttpServletRequestBuilder full(Artwork artwork) {
        return withAdmin(multipart("/api/exhibitions/{e}/artworks/{a}", exhibition.getId(), artwork.getId()));
    }

    private MockMultipartHttpServletRequestBuilder withAdmin(MockMultipartHttpServletRequestBuilder builder) {
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        builder.contextPath("/api").header("Authorization", "Bearer " + adminToken);
        return builder;
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

    private Exhibition exhibition() {
        return exhibitions.saveAndFlush(Exhibition.builder().account(account(Role.EXHIBITION_ADMIN))
                .univName("테스트 대학").deptName("테스트 학과").slug("full-" + UUID.randomUUID()).isPublic(true).build());
    }

    private ExhibitionZone zone(Exhibition exhibition, String name) {
        return zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name(name).orderId(1).build());
    }

    private ArtistProfile join(Artist artist, Exhibition exhibition, ExhibitionArtistStatus status) {
        participations.saveAndFlush(ExhibitionArtistMap.builder()
                .exhibition(exhibition).artist(artist).status(status).build());
        return profiles.saveAndFlush(ArtistProfile.builder()
                .artist(artist).exhibition(exhibition).nameKo(artist.getNameKo()).build());
    }

    private Artwork submittedArtwork(Artist artist, ArtistProfile profile, ExhibitionZone zone, int orderIndex) {
        Artwork artwork = Artwork.builder().title("작품").description("설명").category("회화").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistProfile(profile).artistRole("원래 역할").build());
        artwork.submitTo(zone.getExhibition(), zone);
        artwork.updateOrder(orderIndex);
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        return artworks.saveAndFlush(artwork);
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
