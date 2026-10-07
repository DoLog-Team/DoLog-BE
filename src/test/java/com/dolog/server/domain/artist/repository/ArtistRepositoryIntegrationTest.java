package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ArtistRepositoryIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("dolog_artist_repository_test");

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
    private AccountRepository accountRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("소프트 삭제된 Artist를 accountId로 조회하여 같은 행을 복구한다")
    void findsDeletedArtistByAccountIdAndRestoresSameRow() {
        String suffix = UUID.randomUUID().toString();
        Account account = accountRepository.save(Account.builder()
                .email("artist-" + suffix + "@test.com")
                .socialProvider("GOOGLE")
                .socialProviderId("provider-" + suffix)
                .role(Role.ARTIST_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build());
        Artist artist = artistRepository.save(Artist.builder()
                .account(account)
                .nameKo("삭제 전 이름")
                .phone("01012345678")
                .build());
        UUID artistId = artist.getId();

        artist.softDelete(LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        assertFalse(artistRepository.findById(artistId).isPresent());
        assertEquals(1L, artistRepository.countByPhoneIncludingDeleted("01012345678"));

        Artist deletedArtist = artistRepository
                .findByAccountIdIncludingDeleted(account.getId())
                .orElseThrow();
        assertEquals(artistId, deletedArtist.getId());

        deletedArtist.restore("복구된 이름", "Restored Artist", "01099998888");
        entityManager.flush();
        entityManager.clear();

        Artist restoredArtist = artistRepository.findById(artistId).orElseThrow();
        assertEquals(artistId, restoredArtist.getId());
        assertEquals("복구된 이름", restoredArtist.getNameKo());
        assertEquals("01099998888", restoredArtist.getPhone());
        assertTrue(artistRepository.findByAccountId(account.getId()).isPresent());
    }
}
