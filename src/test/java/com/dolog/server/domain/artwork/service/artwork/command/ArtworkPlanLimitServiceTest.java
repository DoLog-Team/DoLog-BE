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
import com.dolog.server.domain.notification.entity.Notification;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.repository.NotificationRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 플랜 한도는 "출품/등록 순서(createdAt)" 기준으로 센다 — 전시 동선 순서(구역 순서)는 진열용일 뿐,
// 한도를 넘기는 건 항상 "가장 늦게 출품한 작품"이어야 한다 (기획 확인).
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
    @Autowired NotificationRepository notifications;
    @Autowired EntityManager em;

    @Test
    @DisplayName("한도는 출품 순서(createdAt) 기준으로 세고, 동선 순서(구역)는 보지 않는다")
    void countsByCreationOrderNotExhibitionWalkOrder() {
        Account owner = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Exhibition exhibition = exhibitions.saveAndFlush(Exhibition.builder()
                .account(owner).univName("대학").deptName("학과").slug("plan-limit-" + UUID.randomUUID()).build());

        // 구역 A가 동선상 먼저(orderId=1), 구역 B가 나중(orderId=2)
        ExhibitionZone zoneA = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("A").orderId(1).build());
        ExhibitionZone zoneB = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("B").orderId(2).build());

        Artist artist = artists.saveAndFlush(Artist.builder().account(
                accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build())
        ).nameKo("작가").build());

        // 출품 순서: a1(zoneA) → b1(zoneB) → a2(zoneA, orderIndex는 가장 크지만 가장 나중에 출품됨)
        Artwork a1 = submitted(artist, exhibition, zoneA, 10, "A-1");
        Artwork b1 = submitted(artist, exhibition, zoneB, 5, "B-1");
        Artwork a2 = submitted(artist, exhibition, zoneA, 20, "A-2");

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

        // 가장 먼저 출품한 a1, b1은 한도 안. 동선상으로는 zoneA(a1,a2)가 zoneB(b1)보다 먼저지만,
        // 가장 나중에 출품된 a2(동선상 zoneA 안에 있음)가 한도를 초과한다.
        assertFalse(artworks.findById(a1.getId()).orElseThrow().exceedsPlanLimit());
        assertFalse(artworks.findById(b1.getId()).orElseThrow().exceedsPlanLimit());
        assertTrue(artworks.findById(a2.getId()).orElseThrow().exceedsPlanLimit());
    }

    @Test
    @DisplayName("한도 초과로 미노출됐던 작품이 자리가 확보되면 다시 노출되고, 그 작가에게 알림이 간다")
    void notifiesArtistWhenPlanLimitClears() {
        Account owner = accounts.saveAndFlush(Account.builder().role(Role.EXHIBITION_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Exhibition exhibition = exhibitions.saveAndFlush(Exhibition.builder()
                .account(owner).univName("대학").deptName("학과").slug("plan-limit-" + UUID.randomUUID()).build());
        ExhibitionZone zone = zones.saveAndFlush(ExhibitionZone.builder().exhibition(exhibition).name("A").orderId(1).build());

        Account artistAccount = accounts.saveAndFlush(Account.builder().role(Role.ARTIST_ADMIN).accountStatus(AccountStatus.ACTIVE).build());
        Artist artist = artists.saveAndFlush(Artist.builder().account(artistAccount).nameKo("작가").build());

        Artwork a1 = submitted(artist, exhibition, zone, 10, "A-1");
        Artwork a2 = submitted(artist, exhibition, zone, 20, "A-2");

        Plan plan = plans.saveAndFlush(Plan.builder().name("테스트 플랜").maxArtworkCount(1).build());
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

        assertTrue(artworks.findById(a2.getId()).orElseThrow().exceedsPlanLimit());

        List<Notification> exceededNotifications = notifications.findByRecipientIdOrderByCreatedAtDesc(
                owner.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertTrue(exceededNotifications.stream().anyMatch(n ->
                n.getType() == NotificationType.PLAN_LIMIT_EXCEEDED
                        && "1".equals(n.getPayload().get("hiddenArtworkCount"))));

        // a1을 숨겨서 자리를 확보 — 가장 먼저 밀려났던 a2가 다시 노출돼야 한다
        Artwork a1Managed = artworks.findById(a1.getId()).orElseThrow();
        a1Managed.hide(LocalDateTime.now());
        artworks.saveAndFlush(a1Managed);

        service.recompute(exhibition.getId());
        em.flush();
        em.clear();

        assertFalse(artworks.findById(a2.getId()).orElseThrow().exceedsPlanLimit());

        List<Notification> sent = notifications.findByRecipientIdOrderByCreatedAtDesc(
                artistAccount.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        assertTrue(sent.stream().anyMatch(n -> n.getType() == NotificationType.ARTWORK_PLAN_LIMIT_CLEARED));
    }

    private Artwork submitted(Artist artist, Exhibition exhibition, ExhibitionZone zone, int orderIndex, String title) {
        Artwork artwork = Artwork.builder().title(title).description("설명").build();
        artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder().artwork(artwork).artist(artist).artistRole("").build());
        artwork.submitTo(exhibition, zone);
        artwork.updateOrder(orderIndex);
        return artworks.saveAndFlush(artwork);
    }
}
