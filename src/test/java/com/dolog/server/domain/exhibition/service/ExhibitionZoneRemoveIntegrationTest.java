package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkPlanLimitService;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneBulkSaveRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ExhibitionZoneServiceImpl.class, ArtworkFieldRequirement.class, ArtworkPlanLimitService.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExhibitionZoneRemoveIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("dolog_zone_test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
    }

    @Autowired
    private ExhibitionZoneService exhibitionZoneService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private ExhibitionZoneRepository exhibitionZoneRepository;

    @Autowired
    private ExhibitionGuideMapRepository exhibitionGuideMapRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ArtworkArtistMapRepository artworkArtistMapRepository;

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    ExhibitionZoneRemoveIntegrationTest(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("작품이 든 그룹을 빼고 일괄 저장하면 그룹이 삭제되고 그 그룹 작품만 그룹 없음이 된다")
    @SuppressWarnings("unchecked")
    void saveZonesRemovesZoneWithArtworks() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제할 그룹", 2);
        UUID removedArtworkId = f.artwork("그룹 작품", removedZoneId, true);
        UUID keptArtworkId = f.artwork("남는 작품", f.keptZoneId, true);

        ExhibitionZoneListResponse response = exhibitionZoneService.saveZones(
                f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        assertEquals(1, response.getZones().size());
        inTransaction(() -> {
            assertFalse(exhibitionZoneRepository.existsById(removedZoneId));
            assertNull(artworkRepository.findById(removedArtworkId).orElseThrow().getExhibitionZone());
            assertEquals(f.keptZoneId,
                    artworkRepository.findById(keptArtworkId).orElseThrow().getExhibitionZone().getId());
        });

        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_GROUP_REMOVED), payload.capture(), eq(f.exhibitionId));
        assertEquals("그룹 작품", payload.getValue().get("artworkTitle"));
        assertEquals("삭제할 그룹", payload.getValue().get("zoneName"));
        assertEquals(removedArtworkId.toString(), payload.getValue().get("artworkId"));
    }

    @Test
    @DisplayName("계정이 연결되지 않은 작가는 그룹 제거 알림을 건너뛴다")
    void saveZonesSkipsArtistWithoutAccount() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제할 그룹", 2);
        f.artwork("계정 없는 작가 작품", removedZoneId, false);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> assertFalse(exhibitionZoneRepository.existsById(removedZoneId)));
        verify(notificationService, never()).send(any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("그룹 두 개를 한 번에 빼면 두 그룹 작품 모두 그룹 없음이 되고 작품마다 알림이 간다")
    void saveZonesRemovesTwoZones() throws Exception {
        Fixture f = new Fixture();
        UUID firstZoneId = f.zone("첫 그룹", 2);
        UUID secondZoneId = f.zone("둘째 그룹", 3);
        UUID firstArtworkId = f.artwork("첫 작품", firstZoneId, true);
        UUID secondArtworkId = f.artwork("둘째 작품", secondZoneId, true);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> {
            assertFalse(exhibitionZoneRepository.existsById(firstZoneId));
            assertFalse(exhibitionZoneRepository.existsById(secondZoneId));
            assertNull(artworkRepository.findById(firstArtworkId).orElseThrow().getExhibitionZone());
            assertNull(artworkRepository.findById(secondArtworkId).orElseThrow().getExhibitionZone());
        });
        verify(notificationService, times(2)).send(
                any(Account.class), eq(NotificationType.ARTWORK_GROUP_REMOVED), anyMap(), eq(f.exhibitionId));
    }

    @Test
    @DisplayName("빈 그룹을 빼고 일괄 저장하면 알림 없이 그룹만 삭제된다")
    void saveZonesRemovesEmptyZone() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("빈 그룹", 2);

        ExhibitionZoneListResponse response = exhibitionZoneService.saveZones(
                f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        assertEquals(1, response.getZones().size());
        inTransaction(() -> assertFalse(exhibitionZoneRepository.existsById(removedZoneId)));
        verify(notificationService, never()).send(any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("관람 안내도가 묶인 그룹을 빼고 일괄 저장하면 안내도는 남고 그룹 연결만 끊긴다")
    void saveZonesDetachesGuideMap() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("안내도 그룹", 2);
        UUID guideMapId = f.guideMap(removedZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> {
            assertFalse(exhibitionZoneRepository.existsById(removedZoneId));
            ExhibitionGuideMap guideMap = exhibitionGuideMapRepository.findById(guideMapId).orElseThrow();
            assertNull(guideMap.getZone());
        });
    }

    @Test
    @DisplayName("작품과 관람 안내도가 묶인 그룹을 단건 삭제하면 둘 다 그룹 연결이 끊기고 알림이 간다")
    void deleteZoneDetachesArtworkAndGuideMap() {
        Fixture f = new Fixture();
        UUID zoneId = f.zone("단건 삭제 그룹", 2);
        UUID artworkId = f.artwork("단건 작품", zoneId, true);
        UUID guideMapId = f.guideMap(zoneId);

        exhibitionZoneService.deleteZone(zoneId, f.ownerId, false);

        inTransaction(() -> {
            assertFalse(exhibitionZoneRepository.existsById(zoneId));
            assertTrue(exhibitionZoneRepository.existsById(f.keptZoneId));
            assertNull(artworkRepository.findById(artworkId).orElseThrow().getExhibitionZone());
            assertNull(exhibitionGuideMapRepository.findById(guideMapId).orElseThrow().getZone());
        });
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_GROUP_REMOVED), anyMap(), eq(f.exhibitionId));
    }

    @Test
    @DisplayName("그룹이 남아 있는 전시에서 공개 작품의 그룹을 삭제하면 작품은 자동으로 임시저장이 된다")
    void saveZonesDraftsPublishedArtworkWhenOtherZoneRemains() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제할 그룹", 2);
        UUID artworkId = f.publishedArtwork("공개 작품", removedZoneId);
        UUID keptArtworkId = f.publishedArtwork("남는 공개 작품", f.keptZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> {
            Artwork artwork = artworkRepository.findById(artworkId).orElseThrow();
            assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
            assertNotNull(artwork.getAutoDraftedAt());
            assertEquals(ArtworkStatus.PUBLISHED, artworkRepository.findById(keptArtworkId).orElseThrow().getStatus());
        });
    }

    @Test
    @DisplayName("그룹을 모두 삭제하면 그룹이 필수가 아니므로 공개 작품은 공개를 유지한다")
    void saveZonesKeepsPublishedWhenNoZoneRemains() throws Exception {
        Fixture f = new Fixture();
        UUID artworkId = f.publishedArtwork("공개 작품", f.keptZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId,
                new ObjectMapper().readValue("{\"zones\": []}", ExhibitionZoneBulkSaveRequest.class));

        inTransaction(() -> {
            Artwork artwork = artworkRepository.findById(artworkId).orElseThrow();
            assertNull(artwork.getExhibitionZone());
            assertEquals(ArtworkStatus.PUBLISHED, artwork.getStatus());
        });
    }

    @Test
    @DisplayName("그룹 여러 개를 삭제하고 마지막 그룹만 순서를 바꿔 남겨도 저장되고 그룹이 풀린 공개 작품은 임시저장이 된다")
    void saveZonesRemovesSeveralZonesKeepingLastOne() throws Exception {
        Fixture f = new Fixture();
        UUID secondZoneId = f.zone("둘째 그룹", 2);
        UUID lastZoneId = f.zone("마지막 그룹", 3);
        UUID firstArtworkId = f.publishedArtwork("첫 작품", f.keptZoneId);
        UUID secondArtworkId = f.publishedArtwork("둘째 작품", secondZoneId);
        UUID lastArtworkId = f.publishedArtwork("마지막 작품", lastZoneId);
        f.guideMap(secondZoneId);

        String json = """
                {"zones": [{"id": "%s", "name": "마지막 그룹", "orderId": 1}]}
                """.formatted(lastZoneId);
        ExhibitionZoneListResponse response = exhibitionZoneService.saveZones(
                f.exhibitionId, f.ownerId, new ObjectMapper().readValue(json, ExhibitionZoneBulkSaveRequest.class));

        assertEquals(1, response.getZones().size());
        assertEquals(1, response.getZones().get(0).getOrderId());
        inTransaction(() -> {
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(firstArtworkId).orElseThrow().getStatus());
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(secondArtworkId).orElseThrow().getStatus());
            assertEquals(ArtworkStatus.PUBLISHED, artworkRepository.findById(lastArtworkId).orElseThrow().getStatus());
        });
    }

    @Test
    @DisplayName("그룹 삭제로 자동 임시저장된 작품은 남은 그룹까지 지워 그룹이 0개가 되면 다시 공개되고 자동 공개 알림이 간다")
    void saveZonesRepublishesAutoDraftedWhenNoZoneRemains() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("먼저 지울 그룹", 2);
        UUID artworkId = f.publishedArtworkWithArtist("먼저 내려간 작품", removedZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));
        inTransaction(() -> assertEquals(
                ArtworkStatus.DRAFT, artworkRepository.findById(artworkId).orElseThrow().getStatus()));

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, emptyRequest());

        inTransaction(() -> {
            Artwork artwork = artworkRepository.findById(artworkId).orElseThrow();
            assertEquals(ArtworkStatus.PUBLISHED, artwork.getStatus());
            assertNull(artwork.getAutoDraftedAt());
        });
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_REPUBLISHED), anyMap(), eq(f.exhibitionId));
    }

    @Test
    @DisplayName("그룹이 0개가 돼도 작가가 직접 임시저장한 작품과 다른 필수값이 빈 작품은 그대로 둔다")
    void saveZonesKeepsManualOrUnsatisfiedDraftWhenNoZoneRemains() throws Exception {
        Fixture f = new Fixture();
        UUID manualDraftId = f.draftArtwork("작가가 내린 작품", "회화", false);
        UUID unsatisfiedId = f.draftArtwork("카테고리 없는 작품", null, true);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, emptyRequest());

        inTransaction(() -> {
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(manualDraftId).orElseThrow().getStatus());
            Artwork unsatisfied = artworkRepository.findById(unsatisfiedId).orElseThrow();
            assertEquals(ArtworkStatus.DRAFT, unsatisfied.getStatus());
            assertNotNull(unsatisfied.getAutoDraftedAt());
        });
        verify(notificationService, never()).send(
                any(), eq(NotificationType.ARTWORK_REPUBLISHED), anyMap(), any());
    }

    @Test
    @DisplayName("그룹이 남아 있으면 자동 임시저장된 작품을 다시 공개하지 않는다")
    void saveZonesKeepsAutoDraftedWhenZoneRemains() throws Exception {
        Fixture f = new Fixture();
        UUID autoDraftedId = f.draftArtwork("자동으로 내려간 작품", "회화", true, f.keptZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> assertEquals(
                ArtworkStatus.DRAFT, artworkRepository.findById(autoDraftedId).orElseThrow().getStatus()));
    }

    @Test
    @DisplayName("마지막 그룹을 단건 삭제해도 그룹 삭제로 자동 임시저장된 작품이 다시 공개된다")
    void deleteZoneRepublishesAutoDraftedWhenNoZoneRemains() {
        Fixture f = new Fixture();
        UUID autoDraftedId = f.draftArtwork("자동으로 내려간 작품", "회화", true);

        exhibitionZoneService.deleteZone(f.keptZoneId, f.ownerId, false);

        inTransaction(() -> assertEquals(
                ArtworkStatus.PUBLISHED, artworkRepository.findById(autoDraftedId).orElseThrow().getStatus()));
    }

    @Test
    @DisplayName("그룹 0개 전시에 그룹을 단건 생성하면 그룹 없는 공개 작품은 자동 임시저장되고 그룹 필수 알림이 간다")
    @SuppressWarnings("unchecked")
    void createZoneDraftsUngroupedPublishedArtwork() {
        Fixture f = new Fixture();
        f.removeAllZones();
        UUID artworkId = f.publishedArtworkWithArtist("그룹 없는 공개 작품", null, false);

        exhibitionZoneService.createZone(f.exhibitionId, f.ownerId, false, createRequest("새 그룹"));

        inTransaction(() -> {
            Artwork artwork = artworkRepository.findById(artworkId).orElseThrow();
            assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
            assertNotNull(artwork.getAutoDraftedAt());
        });
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_UNPUBLISHED), payload.capture(), eq(f.exhibitionId));
        assertEquals("그룹", payload.getValue().get("requiredFieldNames"));
        assertEquals("그룹 없는 공개 작품", payload.getValue().get("artworkTitle"));
        assertEquals(artworkId.toString(), payload.getValue().get("artworkId"));
    }

    @Test
    @DisplayName("그룹 0개 전시에 일괄 저장으로 그룹을 추가해도 그룹 없는 공개 작품은 자동 임시저장되고, 그룹을 다시 0개로 하면 원래 공개 설정으로 돌아온다")
    void saveZonesDraftsUngroupedPublishedArtworkAndRepublishes() throws Exception {
        Fixture f = new Fixture();
        f.removeAllZones();
        UUID publicId = f.publishedArtworkWithArtist("공개 작품", null, false);
        UUID exhibitionOnlyId = f.publishedArtworkWithArtist("전시에만 공개 작품", null, true);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, new ObjectMapper().readValue(
                "{\"zones\": [{\"name\": \"새 그룹\", \"orderId\": 1}]}", ExhibitionZoneBulkSaveRequest.class));

        inTransaction(() -> {
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(publicId).orElseThrow().getStatus());
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(exhibitionOnlyId).orElseThrow().getStatus());
        });
        verify(notificationService, times(2)).send(
                any(Account.class), eq(NotificationType.ARTWORK_UNPUBLISHED), anyMap(), eq(f.exhibitionId));

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, emptyRequest());

        inTransaction(() -> {
            Artwork publicArtwork = artworkRepository.findById(publicId).orElseThrow();
            assertEquals(ArtworkStatus.PUBLISHED, publicArtwork.getStatus());
            assertFalse(publicArtwork.isExhibitionOnly());
            Artwork exhibitionOnly = artworkRepository.findById(exhibitionOnlyId).orElseThrow();
            assertEquals(ArtworkStatus.PUBLISHED, exhibitionOnly.getStatus());
            assertTrue(exhibitionOnly.isExhibitionOnly());
        });
        verify(notificationService, times(2)).send(
                any(Account.class), eq(NotificationType.ARTWORK_REPUBLISHED), anyMap(), eq(f.exhibitionId));
    }

    @Test
    @DisplayName("그룹을 새로 만들어도 작가가 직접 임시저장한 작품과 그룹이 있는 공개 작품은 그대로 둔다")
    void createZoneKeepsManualDraftAndGroupedPublished() {
        Fixture f = new Fixture();
        UUID groupedId = f.publishedArtwork("그룹 있는 공개 작품", f.keptZoneId);
        UUID manualDraftId = f.draftArtwork("작가가 내린 작품", "회화", false);

        exhibitionZoneService.createZone(f.exhibitionId, f.ownerId, false, createRequest("둘째 그룹"));

        inTransaction(() -> {
            assertEquals(ArtworkStatus.PUBLISHED, artworkRepository.findById(groupedId).orElseThrow().getStatus());
            Artwork manualDraft = artworkRepository.findById(manualDraftId).orElseThrow();
            assertEquals(ArtworkStatus.DRAFT, manualDraft.getStatus());
            assertNull(manualDraft.getAutoDraftedAt());
        });
        verify(notificationService, never()).send(any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("다른 그룹이 남게 단건 삭제하면, 삭제된 그룹 작품은 그룹 제거 알림만, 원래 그룹 없던 공개 작품은 임시저장 알림만 받는다")
    @SuppressWarnings("unchecked")
    void deleteZoneSendsOneNotificationPerArtwork() {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제할 그룹", 2);
        UUID groupedId = f.publishedArtworkWithArtist("그룹 작품", removedZoneId);
        UUID ungroupedId = f.publishedArtworkWithArtist("원래 그룹 없던 작품", null, false);

        exhibitionZoneService.deleteZone(removedZoneId, f.ownerId, false);

        inTransaction(() -> {
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(groupedId).orElseThrow().getStatus());
            assertEquals(ArtworkStatus.DRAFT, artworkRepository.findById(ungroupedId).orElseThrow().getStatus());
        });
        ArgumentCaptor<Map<String, String>> removed = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_GROUP_REMOVED), removed.capture(), eq(f.exhibitionId));
        assertEquals(groupedId.toString(), removed.getValue().get("artworkId"));

        ArgumentCaptor<Map<String, String>> unpublished = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_UNPUBLISHED), unpublished.capture(), eq(f.exhibitionId));
        assertEquals(ungroupedId.toString(), unpublished.getValue().get("artworkId"));
        assertEquals("그룹", unpublished.getValue().get("requiredFieldNames"));

        verify(notificationService, times(2)).send(any(), any(), anyMap(), any());
    }

    @Test
    @DisplayName("관리자가 숨긴 작품도 그룹 없는 공개 작품이면 그룹 생성 때 같이 임시저장된다 (숨김은 그대로)")
    void createZoneDraftsHiddenUngroupedPublishedArtwork() {
        Fixture f = new Fixture();
        f.removeAllZones();
        UUID artworkId = f.publishedArtworkWithArtist("숨긴 작품", null, false);
        write(() -> {
            artworkRepository.findById(artworkId).orElseThrow().hide(LocalDateTime.now());
            return true;
        });

        exhibitionZoneService.createZone(f.exhibitionId, f.ownerId, false, createRequest("새 그룹"));

        inTransaction(() -> {
            Artwork artwork = artworkRepository.findById(artworkId).orElseThrow();
            assertEquals(ArtworkStatus.DRAFT, artwork.getStatus());
            assertNotNull(artwork.getHiddenAt());
        });
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_UNPUBLISHED), anyMap(), eq(f.exhibitionId));
    }

    @Test
    @DisplayName("그룹 삭제로 임시저장된 작품은 그룹 제거 알림만 받고 임시저장 알림은 따로 받지 않는다")
    void saveZonesSendsOnlyGroupRemovedForRemovedZoneArtwork() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제할 그룹", 2);
        UUID artworkId = f.publishedArtworkWithArtist("그룹 작품", removedZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> assertEquals(
                ArtworkStatus.DRAFT, artworkRepository.findById(artworkId).orElseThrow().getStatus()));
        verify(notificationService, times(1)).send(
                any(Account.class), eq(NotificationType.ARTWORK_GROUP_REMOVED), anyMap(), eq(f.exhibitionId));
        verify(notificationService, never()).send(
                any(), eq(NotificationType.ARTWORK_UNPUBLISHED), anyMap(), any());
    }

    @Test
    @DisplayName("그룹 단건 삭제로 그룹이 풀린 공개 작품도 다른 그룹이 남아 있으면 임시저장이 된다")
    void deleteZoneDraftsPublishedArtwork() {
        Fixture f = new Fixture();
        UUID zoneId = f.zone("단건 삭제 그룹", 2);
        UUID artworkId = f.publishedArtwork("공개 작품", zoneId);

        exhibitionZoneService.deleteZone(zoneId, f.ownerId, false);

        inTransaction(() -> assertEquals(
                ArtworkStatus.DRAFT, artworkRepository.findById(artworkId).orElseThrow().getStatus()));
    }

    @Test
    @DisplayName("삭제된 작품이 남아 있는 그룹을 지워도 저장되고, 삭제된 작품도 그룹 연결이 풀린다")
    void saveZonesRemovesZoneWithDeletedArtwork() throws Exception {
        Fixture f = new Fixture();
        UUID removedZoneId = f.zone("삭제된 작품이 있는 그룹", 2);
        UUID deletedArtworkId = f.deletedArtwork("삭제된 작품", removedZoneId);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId, requestKeeping(f.keptZoneId));

        inTransaction(() -> {
            assertFalse(exhibitionZoneRepository.existsById(removedZoneId));
            Object zoneId = entityManager.createNativeQuery("SELECT zone_id FROM artworks WHERE id = :id")
                    .setParameter("id", uuidBytes(deletedArtworkId))
                    .getSingleResult();
            assertNull(zoneId);
        });
    }

    @Test
    @DisplayName("그룹 설명은 1000자까지 저장된다")
    void saveZonesStoresDescriptionUpTo1000() throws Exception {
        Fixture f = new Fixture();
        String description = "가".repeat(1000);
        String json = """
                {"zones": [{"id": "%s", "name": "남길 그룹", "orderId": 1, "description": "%s"}]}
                """.formatted(f.keptZoneId, description);

        exhibitionZoneService.saveZones(f.exhibitionId, f.ownerId,
                new ObjectMapper().readValue(json, ExhibitionZoneBulkSaveRequest.class));

        inTransaction(() -> assertEquals(description,
                exhibitionZoneRepository.findById(f.keptZoneId).orElseThrow().getDescription()));
    }

    private static byte[] uuidBytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private void inTransaction(Runnable assertions) {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.clear();
            assertions.run();
        });
    }

    private ExhibitionZoneBulkSaveRequest requestKeeping(UUID zoneId) throws Exception {
        String json = """
                {"zones": [{"id": "%s", "name": "남길 그룹", "orderId": 1}]}
                """.formatted(zoneId);
        return new ObjectMapper().readValue(json, ExhibitionZoneBulkSaveRequest.class);
    }

    private ExhibitionZoneCreateRequest createRequest(String name) {
        try {
            return new ObjectMapper().readValue("{\"name\": \"%s\"}".formatted(name), ExhibitionZoneCreateRequest.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ExhibitionZoneBulkSaveRequest emptyRequest() throws Exception {
        return new ObjectMapper().readValue("{\"zones\": []}", ExhibitionZoneBulkSaveRequest.class);
    }

    private <T> T write(Supplier<T> work) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> work.get()));
    }

    private class Fixture {
        private final String suffix = UUID.randomUUID().toString();
        private final UUID ownerId;
        private final UUID exhibitionId;
        private final UUID keptZoneId;

        Fixture() {
            ownerId = write(() -> account("owner", Role.EXHIBITION_ADMIN).getId());
            exhibitionId = write(() -> exhibitionRepository.save(Exhibition.builder()
                    .account(accountRepository.getReferenceById(ownerId))
                    .univName("두록대학교")
                    .deptName("시각디자인학과")
                    .slug("zone-" + suffix)
                    .build()).getId());
            keptZoneId = zone("남길 그룹", 1);
        }

        // 그룹 0개 전시로 만든다 (그룹 없이 공개된 작품을 만들 수 있는 상태)
        void removeAllZones() {
            write(() -> {
                exhibitionZoneRepository.deleteAll(exhibitionZoneRepository.findByExhibitionId(exhibitionId));
                return true;
            });
        }

        UUID zone(String name, int orderId) {
            return write(() -> exhibitionZoneRepository.save(ExhibitionZone.builder()
                    .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                    .name(name)
                    .orderId(orderId)
                    .build()).getId());
        }

        UUID artwork(String title, UUID zoneId, boolean withAccount) {
            return write(() -> {
                Artist artist = artistRepository.save(Artist.builder()
                        .account(withAccount ? account("artist-" + UUID.randomUUID(), Role.ARTIST_ADMIN) : null)
                        .nameKo("작가")
                        .build());
                Artwork artwork = artworkRepository.save(Artwork.builder()
                        .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                        .exhibitionZone(exhibitionZoneRepository.getReferenceById(zoneId))
                        .title(title)
                        .build());
                artworkArtistMapRepository.save(ArtworkArtistMap.builder()
                        .artwork(artwork)
                        .artist(artist)
                        .artistRole("작가")
                        .build());
                return artwork.getId();
            });
        }

        UUID publishedArtwork(String title, UUID zoneId) {
            return write(() -> {
                Artwork artwork = artworkRepository.save(Artwork.builder()
                        .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                        .exhibitionZone(exhibitionZoneRepository.getReferenceById(zoneId))
                        .title(title)
                        .category("회화")
                        .productionEndYear(2026)
                        .status(ArtworkStatus.PUBLISHED)
                        .build());
                return artwork.getId();
            });
        }

        UUID publishedArtworkWithArtist(String title, UUID zoneId) {
            return publishedArtworkWithArtist(title, zoneId, false);
        }

        // zoneId 가 null 이면 그룹 없는 공개 작품, exhibitionOnly 면 전시에만 공개
        UUID publishedArtworkWithArtist(String title, UUID zoneId, boolean exhibitionOnly) {
            return write(() -> {
                Artist artist = artistRepository.save(Artist.builder()
                        .account(account("artist-" + UUID.randomUUID(), Role.ARTIST_ADMIN))
                        .nameKo("작가")
                        .build());
                Artwork artwork = artworkRepository.save(Artwork.builder()
                        .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                        .exhibitionZone(zoneId == null ? null : exhibitionZoneRepository.getReferenceById(zoneId))
                        .title(title)
                        .category("회화")
                        .productionEndYear(2026)
                        .status(ArtworkStatus.PUBLISHED)
                        .exhibitionOnly(exhibitionOnly)
                        .build());
                artworkArtistMapRepository.save(ArtworkArtistMap.builder()
                        .artwork(artwork)
                        .artist(artist)
                        .artistRole("작가")
                        .build());
                return artwork.getId();
            });
        }

        // 임시저장 작품. autoDrafted 면 시스템이 내린 작품, 아니면 작가가 직접 내린 작품
        UUID draftArtwork(String title, String category, boolean autoDrafted) {
            return draftArtwork(title, category, autoDrafted, null);
        }

        UUID draftArtwork(String title, String category, boolean autoDrafted, UUID zoneId) {
            return write(() -> {
                Artwork artwork = artworkRepository.save(Artwork.builder()
                        .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                        .exhibitionZone(zoneId == null ? null : exhibitionZoneRepository.getReferenceById(zoneId))
                        .title(title)
                        .category(category)
                        .productionEndYear(2026)
                        .status(ArtworkStatus.DRAFT)
                        .build());
                if (autoDrafted) {
                    artwork.autoDraft(LocalDateTime.now());
                }
                return artwork.getId();
            });
        }

        UUID deletedArtwork(String title, UUID zoneId) {
            return write(() -> {
                Artwork artwork = artworkRepository.save(Artwork.builder()
                        .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                        .exhibitionZone(exhibitionZoneRepository.getReferenceById(zoneId))
                        .title(title)
                        .build());
                artwork.markDeleted(LocalDateTime.now());
                return artwork.getId();
            });
        }

        UUID guideMap(UUID zoneId) {
            return write(() -> exhibitionGuideMapRepository.save(ExhibitionGuideMap.builder()
                    .exhibition(exhibitionRepository.getReferenceById(exhibitionId))
                    .zone(exhibitionZoneRepository.getReferenceById(zoneId))
                    .imageUrl("https://example.com/map.png")
                    .build()).getId());
        }

        private Account account(String prefix, Role role) {
            return accountRepository.save(Account.builder()
                    .email(prefix + "-" + suffix + "@test.com")
                    .password("password")
                    .role(role)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build());
        }
    }
}
