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
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneBulkSaveRequest;
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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
@Import(ExhibitionZoneServiceImpl.class)
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
