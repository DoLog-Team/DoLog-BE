package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.web.dto.response.ArtistListItemResponse;
import com.dolog.server.domain.artist.service.ArtistService;
import com.dolog.server.domain.artist.service.ArtistServiceImpl;
import com.dolog.server.domain.artist.service.ArtistProfileProvisionService;
import com.dolog.server.domain.artist.support.ArtistProfileImageValidator;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.dolog.server.global.util.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ArtistServiceImpl.class, ArtistProfileProvisionService.class})
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

    @Autowired
    private ExhibitionArtistMapRepository participationRepository;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ArtistService artistService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private ArtistProfileImageValidator profileImageValidator;

    @Test
    void eitherUpdateApiChangesNamesReturnedByBothArtistAndProfileReads() throws Exception {
        Exhibition exhibition = nameSearchFixture("이전프로필");
        entityManager.flush();
        entityManager.clear();
        Artist artist = participationRepository.findByExhibitionId(exhibition.getId()).get(0).getArtist();
        UUID artistId = artist.getId();
        UUID accountId = artist.getAccount().getId();
        ArtistProfileUpdateRequest profileRequest = new ArtistProfileUpdateRequest();
        profileRequest.setNameKo("수정프로필");
        profileRequest.setNameEn("Updated Profile");

        artistService.updateMyArtistProfile(accountId, profileRequest, null);
        entityManager.flush();
        entityManager.clear();

        assertEquals("수정프로필", artistService.getArtist(artistId, null).getNameKo());
        assertEquals("Updated Profile", artistService.getArtist(artistId, null).getNameEn());
        assertEquals("수정프로필", artistService.getMyArtistProfile(accountId).nameKo());

        ArtistUpdateRequest basicRequest = new ObjectMapper().readValue(
                "{\"nameKo\":\" 기본수정 \",\"nameEn\":\"Basic Update\"}", ArtistUpdateRequest.class);
        artistService.updateMyArtist(accountId, basicRequest);
        entityManager.flush();
        entityManager.clear();

        assertEquals("기본수정", artistService.getArtist(artistId, null).getNameKo());
        assertEquals("Basic Update", artistService.getMyArtistProfile(accountId).nameEn());
        assertEquals("기본수정", artistService.getMyArtistProfile(accountId).nameKo());

        Account admin = accountRepository.save(Account.builder().email("admin-" + UUID.randomUUID() + "@test.com")
                .role(Role.DOLOG_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        ArtistUpdateRequest adminRequest = new ObjectMapper().readValue(
                "{\"nameEn\":\"Admin Update\"}", ArtistUpdateRequest.class);
        artistService.updateArtist(admin.getId(), artistId, adminRequest);
        entityManager.flush();
        entityManager.clear();

        assertEquals("기본수정", artistService.getMyArtistProfile(accountId).nameKo());
        assertEquals("Admin Update", artistService.getArtist(artistId, null).getNameEn());
    }

    @Test
    void basicNameUpdateAlsoProvisionsMissingCommonProfile() throws Exception {
        Account owner = accountRepository.save(Account.builder().email("missing-" + UUID.randomUUID() + "@test.com")
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artistRepository.save(Artist.builder().account(owner).nameKo("기존이름").build());
        ArtistUpdateRequest request = new ObjectMapper().readValue(
                "{\"nameKo\":\"새이름\"}", ArtistUpdateRequest.class);

        artistService.updateMyArtist(owner.getId(), request);
        entityManager.flush();
        entityManager.clear();

        assertEquals("새이름", artistService.getMyArtistProfile(owner.getId()).nameKo());
        assertEquals(owner.getEmail(), artistService.getMyArtistProfile(owner.getId()).email());
        assertEquals("새이름", artistService.getArtist(artist.getId(), null).getNameKo());
    }

    @Test
    void nameSearchesAndExhibitionListUseCommonProfileInsteadOfBasicName() {
        Exhibition exhibition = nameSearchFixture("새프로필");
        entityManager.flush();
        entityManager.clear();
        PageRequest page = PageRequest.of(0, 10);

        var artists = artistRepository
                .findByProfile_NameKoContainingIgnoreCaseOrProfile_NameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
                        "새프로필", "새프로필", "새프로필", page);
        assertEquals(1, artists.getTotalElements());
        assertEquals("새프로필", ArtistListItemResponse.from(artists.getContent().get(0)).getNameKo());
        assertEquals("Profile Name", ArtistListItemResponse.from(artists.getContent().get(0)).getNameEn());
        assertEquals(1, artistRepository
                .findByProfile_NameKoContainingIgnoreCaseOrProfile_NameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
                        "profile name", "profile name", "profile name", page).getTotalElements());
        assertEquals(0, artistRepository
                .findByProfile_NameKoContainingIgnoreCaseOrProfile_NameEnContainingIgnoreCaseOrAccount_EmailContainingIgnoreCase(
                        "과거기본명", "과거기본명", "과거기본명", page).getTotalElements());

        assertEquals("새프로필", participationRepository.findArtists(exhibition.getId()).get(0).getNameKo());
        assertEquals(1, participationRepository.findArtistsForManagement(
                exhibition.getId(), ExhibitionArtistStatus.JOINED, "새프로필", page).getTotalElements());
        assertEquals(0, participationRepository.findArtistsForManagement(
                exhibition.getId(), ExhibitionArtistStatus.JOINED, "과거기본명", page).getTotalElements());
        assertEquals(1, participationRepository.findAllForManagement(
                null, ExhibitionArtistStatus.JOINED, "Profile Name", page).getTotalElements());
        assertEquals(0, participationRepository.findAllForManagement(
                null, ExhibitionArtistStatus.JOINED, "과거기본명", page).getTotalElements());

        assertEquals(1, artworkRepository.findArtworksBySearch(exhibition.getId(), "새프로필").size());
        assertTrue(artworkRepository.findArtworksBySearch(exhibition.getId(), "과거기본명").isEmpty());
        assertEquals(1, artworkRepository.findAll(ArtworkSpecification.withSearch("새프로필")).size());
        assertTrue(artworkRepository.findAll(ArtworkSpecification.withSearch("과거기본명")).isEmpty());
    }

    @Test
    void nullProfileNameIsNotReplacedByBasicNameInQueries() {
        Exhibition exhibition = nameSearchFixture(null);
        entityManager.flush();
        entityManager.clear();

        assertNull(participationRepository.findArtists(exhibition.getId()).get(0).getNameKo());
        var participation = participationRepository.findArtistsForManagement(
                exhibition.getId(), ExhibitionArtistStatus.JOINED, null, PageRequest.of(0, 10))
                .getContent().get(0);
        assertNull(ArtistListItemResponse.from(participation.getArtist()).getNameKo());
        assertTrue(artworkRepository.findArtworksBySearch(exhibition.getId(), "과거기본명").isEmpty());
    }

    private Exhibition nameSearchFixture(String profileName) {
        String suffix = UUID.randomUUID().toString();
        Account account = accountRepository.save(Account.builder()
                .email("name-" + suffix + "@test.com")
                .socialProvider("GOOGLE").socialProviderId(suffix)
                .role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artistRepository.save(Artist.builder()
                .account(account).nameKo("과거기본명").nameEn("Old Name").build());
        ArtistProfile profile = ArtistProfile.builder().artist(artist)
                .nameKo(profileName).nameEn("Profile Name").build();
        entityManager.persist(profile);
        Exhibition exhibition = Exhibition.builder().account(account)
                .univName("대학교").deptName("학과").slug(suffix).build();
        entityManager.persist(exhibition);
        entityManager.persist(ExhibitionArtistMap.builder().exhibition(exhibition).artist(artist)
                .status(ExhibitionArtistStatus.JOINED).build());
        Artwork artwork = Artwork.builder().exhibition(exhibition).title("작품")
                .status(ArtworkStatus.PUBLISHED).build();
        entityManager.persist(artwork);
        // 이전 작품은 프로필 연결이 없어도 작가의 공통 프로필 이름으로 검색된다.
        entityManager.persist(ArtworkArtistMap.builder().artwork(artwork).artist(artist)
                .artistRole("작가").build());
        return exhibition;
    }

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
