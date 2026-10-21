package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.PlanRepository;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 전시 전체 "동선" 순서(구역 순서 → 구역 안 순서)로 플랜 한도를 센다. orderIndex는 구역마다 따로 매겨지므로
// 구역을 안 보고 orderIndex만 보면, 뒤 구역의 작은 orderIndex가 앞 구역의 큰 orderIndex보다 먼저 뽑히는 버그가 생긴다.
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ArtworkPlanLimitServiceTest {

    @Autowired ArtworkPlanLimitService service;
    @Autowired AccountRepository accounts;
    @Autowired ArtistRepository artists;
    @Autowired ExhibitionRepository exhibitions;
    @Autowired ExhibitionZoneRepository zones;
    @Autowired ArtworkRepository artworks;
    @Autowired PlanRepository plans;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired EntityManager em;

    @Test
    @DisplayName("한도는 구역 순서 → 구역 안 순서(동선) 기준으로 세고, orderIndex만으로 세지 않는다")
    void countsByExhibitionWalkOrderAcrossZones() {
        Account owner = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Exhibition exhibition = exhibitions.saveAndFlush(Exhibition.builder()
                .account(owner).univName("대학").deptName("학과").slug("plan-limit-" + UUID.randomUUID()).build());

        // 구역 A가 동선상 먼저(orderId=1), 구역 B가 나중(orderId=2)
        ExhibitionZone zoneA = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("A").orderId(1).build());
        ExhibitionZone zoneB = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("B").orderId(2).build());

        Artist artist = artists.saveAndFlush(Artist.builder().account(
                accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build())
        ).nameKo("작가").build());

        // 구역 A 안에서 orderIndex 10, 20 (동선상 1,2번째)
        Artwork a1 = submitted(artist, exhibition, zoneA, 10, "A-1");
        Artwork a2 = submitted(artist, exhibition, zoneA, 20, "A-2");
        // 구역 B의 orderIndex는 5로, A 구역 작품들보다 작다 — orderIndex만 보면 B가 먼저로 착각하기 쉽다
        Artwork b1 = submitted(artist, exhibition, zoneB, 5, "B-1");

        Plan plan = plans.saveAndFlush(Plan.builder().name("테스트 플랜").maxArtworkCount(2).build());
        subscriptions.saveAndFlush(Subscription.builder()
                .exhibition(exhibition).plan(plan).status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY).months(1)
                .startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now().plusMonths(1))
                .build());
        em.flush();
        em.clear();

        service.recompute(exhibition.getId());
        em.flush();
        em.clear();

        // 동선 기준 1,2번째(A-1, A-2)는 한도 안, 3번째(B-1)만 초과
        assertFalse(artworks.findById(a1.getId()).orElseThrow().exceedsPlanLimit());
        assertFalse(artworks.findById(a2.getId()).orElseThrow().exceedsPlanLimit());
        assertTrue(artworks.findById(b1.getId()).orElseThrow().exceedsPlanLimit());
    }

    private Artwork submitted(Artist artist, Exhibition exhibition, ExhibitionZone zone, int orderIndex, String title) {
        Artwork artwork = Artwork.builder().title(title).description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(artist).artistRole("").build());
        artwork.submitTo(exhibition, zone);
        artwork.updateOrder(orderIndex);
        return artworks.saveAndFlush(artwork);
    }
}
