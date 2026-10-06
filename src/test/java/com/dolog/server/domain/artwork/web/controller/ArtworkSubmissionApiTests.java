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
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.global.jwt.JwtTokenProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.dolog.server.global.util.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
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
class ArtworkSubmissionApiTests {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired RefreshTokenRepository tokens;
    @Autowired ArtistRepository artists;
    @Autowired ArtistProfileRepository profiles;
    @Autowired ArtworkRepository artworks;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired ExhibitionArtistMapRepository participations;
    @Autowired ExhibitionFieldSettingsRepository fieldSettings;
    @Autowired JwtTokenProvider jwt;
    @Autowired EntityManager em;
    @MockitoBean FileService fileService;

    private Artist me;
    private String myToken;
    private Exhibition exhibition;
    private String ownerToken;
    private ExhibitionZone zone;
    private ArtistProfile myProfile;

    @BeforeEach
    void setUp() {
        reset(fileService);
        me = artist("나작가");
        myToken = token(me.getAccount());
        Account owner = account(Role.EXHIBITION_ADMIN);
        ownerToken = token(owner);
        exhibition = exhibition(owner);
        zone = zone(exhibition, "1구역");
        myProfile = join(me, exhibition, ExhibitionArtistStatus.JOINED);
    }

    // ---------------- 출품 ----------------

    @Test
    @DisplayName("참여 중인 전시의 구역에 출품하면 전시, 구역, 프로필이 연결되고 구역 마지막 순서가 된다")
    void submitsToJoinedExhibition() throws Exception {
        Artwork existing = submitted(artworkOf(me, "먼저 낸 작품"));
        Artwork artwork = artworkOf(me, "새 작품");

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.artworkId").value(artwork.getId().toString()))
                .andExpect(jsonPath("$.data.exhibitionId").value(exhibition.getId().toString()))
                .andExpect(jsonPath("$.data.zoneId").value(zone.getId().toString()))
                .andExpect(jsonPath("$.data.locationMap").value(nullValue()));

        endRequest();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertEquals(exhibition.getId(), found.getExhibition().getId());
        assertEquals(zone.getId(), found.getExhibitionZone().getId());
        assertTrue(found.getOrderIndex() > artworks.findById(existing.getId()).orElseThrow().getOrderIndex());
        assertEquals(myProfile.getId(), found.getArtworkArtistMaps().get(0).getArtistProfile().getId());
        assertEquals(ArtworkStatus.DRAFT, found.getStatus());
    }

    @Test
    @DisplayName("빈 구역에 처음 출품하면 첫 순서(10)를 받는다")
    void firstArtworkInEmptyZone() throws Exception {
        Artwork artwork = artworkOf(me, "첫 작품");

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderIndex").value(10));
    }

    @Test
    @DisplayName("위치 지도를 보내면 artworks/maps 에 올리고 URL 을 저장한다")
    void uploadsLocationMap() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        when(fileService.uploadFile(any(), eq("artworks/maps"))).thenReturn("https://s3/map.webp");

        mvc.perform(withToken(submit(artwork.getId()).file(image("locationMapFile")), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationMap").value("https://s3/map.webp"));
    }

    @Test
    @DisplayName("참여하지 않았거나 승인 대기 중인 전시에는 출품할 수 없다")
    void rejectsNotJoinedExhibition() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        Exhibition notJoined = exhibition(account(Role.EXHIBITION_ADMIN));
        ExhibitionZone notJoinedZone = zone(notJoined, "A");
        Exhibition pending = exhibition(account(Role.EXHIBITION_ADMIN));
        ExhibitionZone pendingZone = zone(pending, "B");
        join(me, pending, ExhibitionArtistStatus.PENDING);

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", notJoined.getId().toString())
                        .param("zoneId", notJoinedZone.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_011"));
        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", pending.getId().toString())
                        .param("zoneId", pendingZone.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_011"));
    }

    @Test
    @DisplayName("다른 전시의 구역으로 출품하면 400")
    void rejectsZoneOfOtherExhibition() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        ExhibitionZone otherZone = zone(exhibition(account(Role.EXHIBITION_ADMIN)), "다른 구역");

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", otherZone.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_012"));
    }

    @Test
    @DisplayName("이미 출품된 작품을 다시 출품하면 409")
    void rejectsAlreadySubmitted() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARTWORK_009"));
    }

    @Test
    @DisplayName("남의 작품은 출품할 수 없고 404, 필수값이 없으면 400")
    void rejectsOthersArtworkAndMissingFields() throws Exception {
        Artist other = artist("남작가");
        Artwork othersArtwork = artworkOf(other, "남의 작품");

        mvc.perform(withToken(submit(othersArtwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARTWORK_001"));
        mvc.perform(withToken(submit(artworkOf(me, "작품").getId()), myToken)
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    // ---------------- 출품 취소 ----------------

    @Test
    @DisplayName("출품을 취소하면 전시에 묶인 정보가 모두 비워지고 공개 상태는 그대로다")
    void cancelClearsExhibitionData() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.updateLocationMap("https://s3/map.webp");
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.hide(LocalDateTime.of(2026, 10, 5, 12, 0));
        em.flush();

        mvc.perform(withToken(delete("/api/artworks/{id}/exhibition", artwork.getId()), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        endRequest();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertNull(found.getExhibition());
        assertNull(found.getExhibitionZone());
        assertNull(found.getOrderIndex());
        assertNull(found.getLocationMap());
        assertNull(found.getHiddenAt());
        assertNull(found.getArtworkArtistMaps().get(0).getArtistProfile());
        assertEquals(me.getId(), found.getArtworkArtistMaps().get(0).getArtist().getId());
        assertEquals(ArtworkStatus.PUBLISHED, found.getStatus());
        // 커밋 전이라 위치 지도 파일은 아직 지우지 않는다
        verify(fileService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("출품되지 않은 작품을 취소하면 400")
    void cancelNotSubmitted() throws Exception {
        Artwork artwork = artworkOf(me, "작품");

        mvc.perform(withToken(delete("/api/artworks/{id}/exhibition", artwork.getId()), myToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_010"));
    }

    // ---------------- 공개 / 비공개 ----------------

    @Test
    @DisplayName("출품 전 작품도 공개로 바꿀 수 있다 (두록 URL 노출)")
    void publishesUnsubmittedArtwork() throws Exception {
        Artwork artwork = artworkOf(me, "작품");

        mvc.perform(withToken(changeStatus(artwork.getId(), "PUBLISHED"), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("출품된 전시의 필수 항목을 채우지 않으면 공개할 수 없고, 채우면 공개된다")
    void requiresFieldSettingsToPublish() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMainImg(true).requiredSize(true).build());

        mvc.perform(withToken(changeStatus(artwork.getId(), "PUBLISHED"), myToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_013"));

        artwork.updateMainImg("https://s3/main.webp");
        artwork.updateSize(new BigDecimal("30"), new BigDecimal("40"), null);
        em.flush();

        mvc.perform(withToken(changeStatus(artwork.getId(), "PUBLISHED"), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("필수 항목이 비어 있어도 비공개(DRAFT)로는 바꿀 수 있다")
    void draftIgnoresFieldSettings() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMainImg(true).build());

        mvc.perform(withToken(changeStatus(artwork.getId(), "DRAFT"), myToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("전시 어드민이 숨긴 작품을 작가가 공개해도 숨김은 유지된다")
    void publishKeepsHidden() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        LocalDateTime hiddenAt = LocalDateTime.of(2026, 10, 5, 12, 0);
        artwork.hide(hiddenAt);
        em.flush();

        mvc.perform(withToken(changeStatus(artwork.getId(), "PUBLISHED"), myToken))
                .andExpect(status().isOk());

        endRequest();
        assertEquals(hiddenAt, artworks.findById(artwork.getId()).orElseThrow().getHiddenAt());
    }

    @Test
    @DisplayName("없는 상태값이나 남의 작품은 거절한다")
    void rejectsInvalidStatusRequests() throws Exception {
        Artwork artwork = artworkOf(me, "작품");
        Artwork othersArtwork = artworkOf(artist("남작가"), "남의 작품");

        mvc.perform(withToken(changeStatus(artwork.getId(), "HIDDEN"), myToken))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(changeStatus(othersArtwork.getId(), "PUBLISHED"), myToken))
                .andExpect(status().isNotFound());
    }

    // ---------------- 숨김 / 재공개 ----------------

    @Test
    @DisplayName("전시 소유 어드민이 숨기면 숨김 시각이 기록되고, 재공개하면 비워진다. 공개 상태는 그대로다")
    void ownerHidesAndUnhides() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        em.flush();

        mvc.perform(withToken(changeHidden(artwork.getId(), true), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true))
                .andExpect(jsonPath("$.data.hiddenAt").isNotEmpty());
        endRequest();
        Artwork hidden = artworks.findById(artwork.getId()).orElseThrow();
        assertTrue(hidden.isHidden());
        assertEquals(ArtworkStatus.PUBLISHED, hidden.getStatus());

        mvc.perform(withToken(changeHidden(artwork.getId(), false), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(false))
                .andExpect(jsonPath("$.data.hiddenAt").value(nullValue()));
        endRequest();
        assertFalse(artworks.findById(artwork.getId()).orElseThrow().isHidden());
    }

    @Test
    @DisplayName("이미 숨긴 작품을 다시 숨겨도 처음 숨긴 시각은 유지된다")
    void hideIsIdempotent() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        LocalDateTime first = LocalDateTime.of(2026, 10, 1, 9, 0);
        artwork.hide(first);
        em.flush();

        mvc.perform(withToken(changeHidden(artwork.getId(), true), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hiddenAt").value("2026-10-01 09:00:00"));
    }

    @Test
    @DisplayName("다른 전시의 어드민은 403, 작가 어드민은 403, 출품 전 작품은 400")
    void hideRejections() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        Artwork unsubmitted = artworkOf(me, "미출품");
        Account otherOwner = account(Role.EXHIBITION_ADMIN);
        exhibition(otherOwner);

        mvc.perform(withToken(changeHidden(artwork.getId(), true), token(otherOwner)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARTWORK_014"));
        mvc.perform(withToken(changeHidden(artwork.getId(), true), myToken))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(changeHidden(unsubmitted.getId(), true), ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_010"));

        endRequest();
        assertFalse(artworks.findById(artwork.getId()).orElseThrow().isHidden());
    }


    // ---------------- 리뷰 반영: 필수 항목 일관성 ----------------

    @Test
    @DisplayName("이미 공개된 작품을 필수 항목을 못 채운 채 출품하면 출품은 되고 비공개(DRAFT)로 내려간다")
    void publishedArtworkFallsBackToDraftOnSubmit() throws Exception {
        Artwork artwork = artworkOf(me, "공개 작품");
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMainImg(true).build());

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        endRequest();
        assertEquals(ArtworkStatus.DRAFT, artworks.findById(artwork.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("필수 항목을 채운 공개 작품은 출품 후에도 공개 상태를 유지한다")
    void satisfiedPublishedArtworkStaysPublished() throws Exception {
        Artwork artwork = artworkOf(me, "공개 작품");
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        artwork.updateMainImg("https://s3/main.webp");
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMainImg(true).build());

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("출품된 공개 작품에서 필수 항목을 지우는 수정은 400 이고 아무것도 바뀌지 않는다")
    void updateCannotBreakRequiredFieldsOfPublishedArtwork() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.replaceMaterials(java.util.List.of("백자토"));
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMaterials(true).build());
        em.flush();

        mvc.perform(withToken(patchArtwork(artwork.getId()), myToken).param("materials", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARTWORK_013"));
    }

    @Test
    @DisplayName("비공개이거나 출품 전인 작품은 필수 항목을 지우는 수정도 허용한다")
    void updateAllowedWhenDraftOrUnsubmitted() throws Exception {
        fieldSettings.saveAndFlush(ExhibitionFieldSettings.builder()
                .exhibition(exhibition).requiredMaterials(true).build());
        Artwork draft = submitted(artworkOf(me, "비공개 출품작"));
        draft.replaceMaterials(java.util.List.of("백자토"));
        Artwork unsubmitted = artworkOf(me, "출품 전 공개작");
        unsubmitted.replaceMaterials(java.util.List.of("백자토"));
        unsubmitted.changeStatus(ArtworkStatus.PUBLISHED);
        em.flush();

        mvc.perform(withToken(patchArtwork(draft.getId()), myToken).param("materials", ""))
                .andExpect(status().isOk());
        mvc.perform(withToken(patchArtwork(unsubmitted.getId()), myToken).param("materials", ""))
                .andExpect(status().isOk());
    }

    // ---------------- 리뷰 반영: 재출품, 공동 작가, 지우님 경로 ----------------

    @Test
    @DisplayName("출품 취소 후 다른 전시에 다시 출품하면 이전 전시의 숨김, 프로필, 순서가 따라오지 않는다")
    void resubmitToOtherExhibitionStartsClean() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.updateOrder(990);
        artwork.hide(LocalDateTime.of(2026, 10, 5, 12, 0));
        em.flush();
        Exhibition second = exhibition(account(Role.EXHIBITION_ADMIN));
        ExhibitionZone secondZone = zone(second, "2관");
        ArtistProfile secondProfile = join(me, second, ExhibitionArtistStatus.JOINED);

        mvc.perform(withToken(delete("/api/artworks/{id}/exhibition", artwork.getId()), myToken))
                .andExpect(status().isOk());
        endRequest();
        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", second.getId().toString())
                        .param("zoneId", secondZone.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderIndex").value(10));

        endRequest();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertEquals(second.getId(), found.getExhibition().getId());
        assertNull(found.getHiddenAt());
        assertEquals(secondProfile.getId(), found.getArtworkArtistMaps().get(0).getArtistProfile().getId());
    }

    @Test
    @DisplayName("공동 작품을 출품하면 그 전시 프로필이 있는 공동 작가만 프로필이 연결된다")
    void submitLinksCoArtistProfilesWhenPresent() throws Exception {
        Artist joinedCoArtist = artist("참여 공동작가");
        ArtistProfile coProfile = join(joinedCoArtist, exhibition, ExhibitionArtistStatus.JOINED);
        Artist outsider = artist("미참여 공동작가");
        Artwork artwork = artworkOf(me, "공동 작품");
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(joinedCoArtist).artistRole("").build());
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(outsider).artistRole("").build());
        artworks.saveAndFlush(artwork);

        mvc.perform(withToken(submit(artwork.getId()), myToken)
                        .param("exhibitionId", exhibition.getId().toString())
                        .param("zoneId", zone.getId().toString()))
                .andExpect(status().isOk());

        endRequest();
        java.util.Map<UUID, ArtistProfile> linked = new java.util.HashMap<>();
        artworks.findById(artwork.getId()).orElseThrow().getArtworkArtistMaps()
                .forEach(map -> linked.put(map.getArtist().getId(), map.getArtistProfile()));
        assertEquals(myProfile.getId(), linked.get(me.getId()).getId());
        assertEquals(coProfile.getId(), linked.get(joinedCoArtist.getId()).getId());
        assertNull(linked.get(outsider.getId()));
    }

    @Test
    @DisplayName("전시 어드민이 작가를 제외해 자동 출품 취소되면 위치 지도, 프로필, 숨김까지 비워진다")
    void removedArtistAutoCancelClearsEverything() throws Exception {
        Artwork artwork = submitted(artworkOf(me, "작품"));
        artwork.updateLocationMap("https://s3/map.webp");
        artwork.hide(LocalDateTime.of(2026, 10, 5, 12, 0));
        em.flush();

        mvc.perform(withToken(delete("/api/exhibitions/{exhibitionId}/artists/{artistId}",
                        exhibition.getId(), me.getId()), ownerToken))
                .andExpect(status().isOk());

        // 테스트 트랜잭션은 커밋되지 않으므로, 커밋 후 파일 삭제가 예약됐는지는 등록된 콜백을 직접 실행해서 확인한다.
        verify(fileService, never()).deleteFile(any());
        TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(sync -> sync.getClass().getName().startsWith(ArtworkFileHandler.class.getName()))
                .forEach(TransactionSynchronization::afterCommit);
        verify(fileService).deleteFile("https://s3/map.webp");

        endRequest();
        Artwork found = artworks.findById(artwork.getId()).orElseThrow();
        assertNull(found.getExhibition());
        assertNull(found.getLocationMap());
        assertNull(found.getHiddenAt());
        assertNull(found.getOrderIndex());
        assertNull(found.getArtworkArtistMaps().get(0).getArtistProfile());
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

    private Exhibition exhibition(Account owner) {
        return exhibitions.saveAndFlush(Exhibition.builder().account(owner).univName("테스트 대학")
                .deptName("테스트 학과").slug("submission-" + UUID.randomUUID()).build());
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

    private Artwork artworkOf(Artist artist, String title) {
        Artwork artwork = Artwork.builder().title(title).description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                .artwork(artwork).artist(artist).artistRole("").build());
        return artworks.saveAndFlush(artwork);
    }

    private Artwork submitted(Artwork artwork) {
        artwork.submitTo(exhibition, zone);
        artwork.getArtworkArtistMaps().get(0).linkProfile(myProfile);
        artwork.updateOrder(10);
        return artworks.saveAndFlush(artwork);
    }

    private MockMultipartHttpServletRequestBuilder submit(UUID artworkId) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/artworks/{id}/exhibition", artworkId);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        return builder;
    }

    private MockMultipartHttpServletRequestBuilder patchArtwork(UUID artworkId) {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/artworks/{id}", artworkId);
        builder.with(request -> {
            request.setMethod("PATCH");
            return request;
        });
        return builder;
    }

    private MockHttpServletRequestBuilder changeStatus(UUID artworkId, String status) {
        return patch("/api/artworks/{id}/exhibition", artworkId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}");
    }

    private MockHttpServletRequestBuilder changeHidden(UUID artworkId, boolean hidden) {
        return patch("/api/artworks/{id}/hidden", artworkId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"hidden\":" + hidden + "}");
    }

    private <T extends MockHttpServletRequestBuilder> T withToken(T builder, String token) {
        builder.contextPath("/api").header("Authorization", "Bearer " + token);
        return builder;
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, "map.png", "image/png", new byte[]{1, 2, 3});
    }

    // 실제 요청은 요청마다 커밋되므로 테스트에서도 요청 사이에 flush/clear 로 맞춘다.
    private void endRequest() {
        em.flush();
        em.clear();
    }
}
