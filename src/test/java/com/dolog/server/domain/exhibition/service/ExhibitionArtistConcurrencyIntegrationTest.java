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
import com.dolog.server.domain.artwork.support.ArtworkSubmissionCanceller;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.global.util.FileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ExhibitionArtistServiceImpl.class,
        ArtworkSubmissionCanceller.class,
        ArtworkFileHandler.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExhibitionArtistConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("dolog_concurrency_test");

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
    private ExhibitionArtistService exhibitionArtistService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Autowired
    private ExhibitionZoneRepository exhibitionZoneRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ArtworkArtistMapRepository artworkArtistMapRepository;

    @MockitoBean
    private FileService fileService;

    @Autowired
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    ExhibitionArtistConcurrencyIntegrationTest(
            PlatformTransactionManager transactionManager
    ) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @RepeatedTest(10)
    @DisplayName("DELETE로 공동 작가를 동시에 제외하면 마지막 작가 처리 후 출품이 취소된다")
    void concurrentDeleteRemovalsCancelJointArtwork() throws Exception {
        Scenario scenario = createScenario();

        runConcurrently(
                () -> exhibitionArtistService.removeArtistFromExhibition(
                        scenario.ownerId(),
                        scenario.exhibitionId(),
                        scenario.firstArtistId()
                ),
                () -> exhibitionArtistService.removeArtistFromExhibition(
                        scenario.ownerId(),
                        scenario.exhibitionId(),
                        scenario.secondArtistId()
                )
        );

        assertAllArtistsRemovedAndArtworkCancelled(scenario);
    }

    @RepeatedTest(10)
    @DisplayName("PATCH로 공동 작가를 동시에 제외하면 마지막 작가 처리 후 출품이 취소된다")
    void concurrentPatchRemovalsCancelJointArtwork() throws Exception {
        Scenario scenario = createScenario();

        runConcurrently(
                () -> exhibitionArtistService.updateArtistStatuses(
                        scenario.ownerId(),
                        scenario.exhibitionId(),
                        List.of(scenario.firstArtistId()),
                        ExhibitionArtistStatus.REMOVED
                ),
                () -> exhibitionArtistService.updateArtistStatuses(
                        scenario.ownerId(),
                        scenario.exhibitionId(),
                        List.of(scenario.secondArtistId()),
                        ExhibitionArtistStatus.REMOVED
                )
        );

        assertAllArtistsRemovedAndArtworkCancelled(scenario);
    }

    private Scenario createScenario() {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString();

            Account owner = accountRepository.save(Account.builder()
                    .email("owner-" + suffix + "@test.com")
                    .password("password")
                    .role(Role.EXHIBITION_ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build());

            Account firstArtistAccount = accountRepository.save(
                    artistAccount("first-" + suffix + "@test.com")
            );
            Account secondArtistAccount = accountRepository.save(
                    artistAccount("second-" + suffix + "@test.com")
            );

            Artist firstArtist = artistRepository.save(Artist.builder()
                    .account(firstArtistAccount)
                    .nameKo("공동 작가 A")
                    .build());
            Artist secondArtist = artistRepository.save(Artist.builder()
                    .account(secondArtistAccount)
                    .nameKo("공동 작가 B")
                    .build());

            Exhibition exhibition = exhibitionRepository.save(
                    Exhibition.builder()
                            .account(owner)
                            .univName("두록대학교")
                            .deptName("시각디자인학과")
                            .slug("concurrency-" + suffix)
                            .build()
            );

            ExhibitionZone zone = exhibitionZoneRepository.save(
                    ExhibitionZone.builder()
                            .exhibition(exhibition)
                            .name("공동 작품 전시 구역")
                            .orderId(1)
                            .build()
            );

            exhibitionArtistMapRepository.saveAll(List.of(
                    joinedMap(exhibition, firstArtist),
                    joinedMap(exhibition, secondArtist)
            ));

            Artwork artwork = artworkRepository.save(Artwork.builder()
                    .exhibition(exhibition)
                    .exhibitionZone(zone)
                    .title("공동 작품")
                    .build());

            artworkArtistMapRepository.saveAll(List.of(
                    artworkArtistMap(artwork, firstArtist),
                    artworkArtistMap(artwork, secondArtist)
            ));

            entityManager.flush();

            return new Scenario(
                    owner.getId(),
                    exhibition.getId(),
                    firstArtist.getId(),
                    secondArtist.getId(),
                    artwork.getId()
            );
        }));
    }

    private Account artistAccount(String email) {
        return Account.builder()
                .email(email)
                .password("password")
                .role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private ExhibitionArtistMap joinedMap(
            Exhibition exhibition,
            Artist artist
    ) {
        return ExhibitionArtistMap.builder()
                .exhibition(exhibition)
                .artist(artist)
                .status(ExhibitionArtistStatus.JOINED)
                .build();
    }

    private ArtworkArtistMap artworkArtistMap(
            Artwork artwork,
            Artist artist
    ) {
        return ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(artist)
                .artistRole("공동 작가")
                .build();
    }

    private void runConcurrently(
            CheckedAction firstAction,
            CheckedAction secondAction
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Void> firstTask = concurrentTask(
                ready,
                start,
                firstAction
        );
        Callable<Void> secondTask = concurrentTask(
                ready,
                start,
                secondAction
        );

        Future<Void> first = executor.submit(firstTask);
        Future<Void> second = executor.submit(secondTask);

        try {
            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "두 제외 요청이 제한 시간 안에 준비되어야 합니다."
            );
            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Void> concurrentTask(
            CountDownLatch ready,
            CountDownLatch start,
            CheckedAction action
    ) {
        return () -> {
            ready.countDown();
            assertTrue(
                    start.await(5, TimeUnit.SECONDS),
                    "동시 실행 신호를 제한 시간 안에 받아야 합니다."
            );
            action.run();
            return null;
        };
    }

    private void assertAllArtistsRemovedAndArtworkCancelled(
            Scenario scenario
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.clear();

            ExhibitionArtistMap firstMap = exhibitionArtistMapRepository
                    .findByExhibitionIdAndArtistId(
                            scenario.exhibitionId(),
                            scenario.firstArtistId()
                    )
                    .orElseThrow();
            ExhibitionArtistMap secondMap = exhibitionArtistMapRepository
                    .findByExhibitionIdAndArtistId(
                            scenario.exhibitionId(),
                            scenario.secondArtistId()
                    )
                    .orElseThrow();
            Artwork artwork = artworkRepository
                    .findById(scenario.artworkId())
                    .orElseThrow();

            assertEquals(ExhibitionArtistStatus.REMOVED, firstMap.getStatus());
            assertEquals(ExhibitionArtistStatus.REMOVED, secondMap.getStatus());
            assertNull(artwork.getExhibition());
            assertNull(artwork.getExhibitionZone());
        });
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private record Scenario(
            UUID ownerId,
            UUID exhibitionId,
            UUID firstArtistId,
            UUID secondArtistId,
            UUID artworkId
    ) {
    }
}
