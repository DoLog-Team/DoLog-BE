package com.dolog.server.domain.artwork;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkMaterial;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ArtworkSchemaTests {
    @Autowired AccountRepository accounts;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ArtworkRepository artworks;
    @Autowired EntityManager em;

    private Exhibition exhibition;

    @BeforeEach
    void setUp() {
        Account account = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build());
        exhibition = exhibitions.saveAndFlush(Exhibition.builder().account(account).univName("테스트 대학")
                .deptName("테스트 학과").slug("artwork-schema-" + UUID.randomUUID()).build());
    }

    @Test
    @DisplayName("상태를 지정하지 않고 저장하면 DB 에 DRAFT, 조회수 0, hidden_at null 로 들어간다")
    void savesDefaultStatus() {
        UUID id = artworks.saveAndFlush(Artwork.builder().exhibition(exhibition).title("작품").build()).getId();
        em.clear();

        Object[] row = (Object[]) em.createNativeQuery(
                        "SELECT status, view_count, hidden_at FROM artworks WHERE id = UUID_TO_BIN(:id)")
                .setParameter("id", id.toString())
                .getSingleResult();

        assertEquals("DRAFT", row[0]);
        assertEquals(0L, ((Number) row[1]).longValue());
        assertNull(row[2]);
    }

    @Test
    @DisplayName("V5 신규 컬럼이 저장 후 다시 읽어도 그대로 유지된다")
    void roundTripsV2Columns() {
        UUID id = artworks.saveAndFlush(Artwork.builder()
                .exhibition(exhibition)
                .title("파도의 그릇")
                .shortIntro("물결을 형상화한 백자 그릇")
                .width(new BigDecimal("32.50"))
                .height(new BigDecimal("32.00"))
                .depth(new BigDecimal("7.25"))
                .productionStartYear(2026)
                .productionEndYear(2026)
                .productionEndMonth(6)
                .productionEndDay(15)
                .purchaseChatUrl("https://open.kakao.com/o/test")
                .showPurchaseButton(true)
                .build()).getId();
        em.clear();

        Artwork found = artworks.findById(id).orElseThrow();

        assertEquals("물결을 형상화한 백자 그릇", found.getShortIntro());
        assertEquals(0, new BigDecimal("32.50").compareTo(found.getWidth()));
        assertEquals(0, new BigDecimal("32.00").compareTo(found.getHeight()));
        assertEquals(0, new BigDecimal("7.25").compareTo(found.getDepth()));
        assertEquals(2026, found.getProductionStartYear());
        assertNull(found.getProductionStartMonth());
        assertNull(found.getProductionStartDay());
        assertEquals(2026, found.getProductionEndYear());
        assertEquals(6, found.getProductionEndMonth());
        assertEquals(15, found.getProductionEndDay());
        assertEquals("https://open.kakao.com/o/test", found.getPurchaseChatUrl());
        assertTrue(found.getShowPurchaseButton());
    }

    @Test
    @DisplayName("공개 상태와 숨김 시각이 DB 에 저장되고 다시 읽힌다")
    void persistsStatusAndHiddenAt() {
        Artwork artwork = Artwork.builder().exhibition(exhibition).title("작품").build();
        artwork.changeStatus(ArtworkStatus.PUBLISHED);
        LocalDateTime hiddenAt = LocalDateTime.of(2026, 10, 3, 12, 30, 15);
        artwork.hide(hiddenAt);
        UUID id = artworks.saveAndFlush(artwork).getId();
        em.clear();

        Artwork found = artworks.findById(id).orElseThrow();

        assertEquals(ArtworkStatus.PUBLISHED, found.getStatus());
        assertEquals(hiddenAt, found.getHiddenAt());
        assertTrue(found.isVisibleOnDolog());
        assertFalse(found.isVisibleOnExhibition());
    }

    @Test
    @DisplayName("재료를 교체하면 이전 재료 행은 지워지고 새 재료가 순서대로 저장된다")
    void replacesMaterialRows() {
        Artwork artwork = Artwork.builder().exhibition(exhibition).title("작품").build();
        artwork.replaceMaterials(List.of("백자토", "청화안료"));
        UUID id = artworks.saveAndFlush(artwork).getId();

        artwork.replaceMaterials(List.of("유약", "백자토"));
        artworks.saveAndFlush(artwork);
        em.clear();

        List<ArtworkMaterial> rows = em.createQuery(
                        "SELECT m FROM ArtworkMaterial m WHERE m.artwork.id = :id ORDER BY m.orderIndex",
                        ArtworkMaterial.class)
                .setParameter("id", id)
                .getResultList();
        assertEquals(List.of("유약", "백자토"), rows.stream().map(ArtworkMaterial::getName).toList());
        assertEquals(List.of(1, 2), rows.stream().map(ArtworkMaterial::getOrderIndex).toList());
        assertTrue(rows.stream().allMatch(m -> m.getCreatedAt() != null));
    }

    @Test
    @DisplayName("재료가 있는 작품을 삭제하면 재료도 함께 삭제된다")
    void deletingArtworkDeletesMaterials() {
        Artwork artwork = Artwork.builder().exhibition(exhibition).title("작품").build();
        artwork.replaceMaterials(List.of("백자토"));
        UUID id = artworks.saveAndFlush(artwork).getId();

        artworks.delete(artwork);
        artworks.flush();
        em.clear();

        Long remaining = em.createQuery(
                        "SELECT COUNT(m) FROM ArtworkMaterial m WHERE m.artwork.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        assertEquals(0L, remaining);
        assertTrue(artworks.findById(id).isEmpty());
    }
}
