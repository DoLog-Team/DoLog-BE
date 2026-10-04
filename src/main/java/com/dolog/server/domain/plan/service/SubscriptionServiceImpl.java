package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.PlanPrice;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.SubscriptionRefund;
import com.dolog.server.domain.plan.entity.SystemSetting;
import com.dolog.server.domain.plan.entity.enums.PlanChangeType;
import com.dolog.server.domain.plan.entity.enums.RefundStatus;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.exception.PlanErrorCode;
import com.dolog.server.domain.plan.exception.PlanException;
import com.dolog.server.domain.plan.exception.SubscriptionErrorCode;
import com.dolog.server.domain.plan.exception.SubscriptionException;
import com.dolog.server.domain.plan.repository.PlanRepository;
import com.dolog.server.domain.plan.repository.SubscriptionRefundRepository;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import com.dolog.server.domain.plan.repository.SystemSettingRepository;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPlanChangeRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionPolicyUpdateRequest;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionStatusUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCancelResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionMyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPlanChangeResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionPolicyResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionRefundResponse;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionStatusUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final List<SubscriptionStatus> ONGOING_STATUSES =
            List.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.ACTIVE);

    public static final String CANCEL_GRACE_DAYS_KEY = "SUBSCRIPTION_CANCEL_GRACE_DAYS";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionRefundRepository subscriptionRefundRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional
    public SubscriptionCreateResponse createSubscription(UUID exhibitionId, UUID accountId, SubscriptionCreateRequest request) {
        // 잠금을 먼저 걸어야, 동시에 들어온 신청이 서로의 PENDING_PAYMENT 구독을 보지 못하는 상황을 막을 수 있다
        Exhibition exhibition = lockOwnedExhibitionOrThrow(exhibitionId, accountId);

        boolean alreadySubscribed = subscriptionRepository.existsByExhibitionIdAndStatusIn(
                exhibitionId, ONGOING_STATUSES
        );
        if (alreadySubscribed) {
            throw new SubscriptionException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));
        requireActivePlan(plan);

        PlanPrice planPrice = plan.findPriceByCycle(request.getBillingCycle())
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PLAN_PRICE_NOT_FOUND));

        Subscription subscription = Subscription.builder()
                .exhibition(exhibition)
                .plan(plan)
                .billingCycle(request.getBillingCycle())
                .months(planPrice.getMonths())
                .paidAmount(planPrice.resolveDiscountedPrice())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        return SubscriptionCreateResponse.of(saved, planPrice.getPrice());
    }

    @Override
    public List<SubscriptionMyResponse> getMySubscriptions(UUID accountId) {
        return exhibitionRepository.findByAccountId(accountId)
                .flatMap(exhibition -> subscriptionRepository
                        .findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(exhibition.getId(), ONGOING_STATUSES))
                .map(this::toResponse)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public SubscriptionPlanChangeResponse changePlan(UUID exhibitionId, UUID accountId, SubscriptionPlanChangeRequest request) {
        findOwnedExhibitionOrThrow(exhibitionId, accountId);
        Subscription subscription = findCurrentSubscriptionOrThrow(exhibitionId);

        Plan previousPlan = subscription.getPlan();
        UUID previousPlanId = previousPlan.getId();
        String previousPlanName = previousPlan.getName();

        Plan targetPlan = planRepository.findById(request.getTargetPlanId())
                .orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

        requireActivePlan(targetPlan);

        PlanPrice targetPrice = targetPlan.findPriceByCycle(request.getBillingCycle())
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PLAN_PRICE_NOT_FOUND));

        PlanChangeType changeType = resolveChangeType(previousPlan, targetPlan);

        subscription.changePlan(targetPlan, request.getBillingCycle(), targetPrice.getMonths());

        // 결제 전에는 바뀐 플랜 기준 금액으로 결제되므로 결제금액도 갱신한다 (활성 구독은 차액 정산 정책 확정 후 처리)
        if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
            subscription.updatePaidAmount(targetPrice.resolveDiscountedPrice());
        }

        // TODO: Artwork 도메인에 "플랜 한도 초과 미노출" 자동 전환 기능이 생기면 여기서 호출 연동 필요
        // (다운그레이드 시 연결 순서 기준 초과분 자동 미노출 / 업그레이드 시 자동 재공개 — 피그마 "작품 수 초과에 따른 예외처리" 참고)

        return SubscriptionPlanChangeResponse.of(subscription, previousPlanId, previousPlanName, changeType);
    }

    @Override
    @Transactional
    public SubscriptionCancelResponse cancelSubscription(UUID exhibitionId, UUID accountId) {
        lockOwnedExhibitionOrThrow(exhibitionId, accountId);
        Subscription subscription = findCurrentSubscriptionOrThrow(exhibitionId);
        LocalDateTime now = LocalDateTime.now();

        // 결제 전 구독은 환불할 금액이 없고, 전시의 만료일도 건드리지 않는다
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            subscription.cancelSubscription(now);
            return SubscriptionCancelResponse.from(subscription, null);
        }

        // 해지 후 유예기간 끝(해당 날짜 23:59:59)까지 사용 가능, 그 이후 전시는 만료된다
        LocalDateTime usageEndAt = LocalDate.now().plusDays(getCancelGraceDays()).atTime(LocalTime.of(23, 59, 59));

        // 환불 계산은 계획된 종료일(endedAt)이 바뀌기 전에 해야 한다. 사용 일수는 해지한 날까지만 센다
        SubscriptionRefund refund = SubscriptionRefund.calculate(subscription, now, now);
        subscriptionRefundRepository.save(refund);

        subscription.cancelSubscription(usageEndAt);
        subscription.getExhibition().applySubscriptionEnd(usageEndAt);

        return SubscriptionCancelResponse.from(subscription, refund.getRefundAmount());
    }

    @Override
    public SubscriptionPolicyResponse getCancelPolicy() {
        return SubscriptionPolicyResponse.of(getCancelGraceDays());
    }

    @Override
    @Transactional
    public SubscriptionPolicyResponse updateCancelPolicy(SubscriptionPolicyUpdateRequest request) {
        int days = request.getCancelGraceDays();

        SystemSetting setting = systemSettingRepository.findById(CANCEL_GRACE_DAYS_KEY)
                .orElseGet(() -> SystemSetting.builder().key(CANCEL_GRACE_DAYS_KEY).value("0").build());
        setting.updateValue(String.valueOf(days));
        systemSettingRepository.save(setting);

        return SubscriptionPolicyResponse.of(days);
    }

    @Override
    public List<SubscriptionRefundResponse> getRefunds(RefundStatus status) {
        List<SubscriptionRefund> refunds = status != null
                ? subscriptionRefundRepository.findByStatusOrderByRequestedAtDesc(status)
                : subscriptionRefundRepository.findAllByOrderByRequestedAtDesc();

        return refunds.stream()
                .map(SubscriptionRefundResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionRefundResponse completeRefund(UUID refundId) {
        SubscriptionRefund refund = subscriptionRefundRepository.findById(refundId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_REFUND_NOT_FOUND));

        if (refund.getStatus() == RefundStatus.COMPLETED) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_REFUND_ALREADY_COMPLETED);
        }

        refund.complete(LocalDateTime.now());

        return SubscriptionRefundResponse.from(refund);
    }

    private int getCancelGraceDays() {
        return systemSettingRepository.findById(CANCEL_GRACE_DAYS_KEY)
                .map(setting -> Integer.parseInt(setting.getValue()))
                .orElse(0);
    }

    @Override
    public List<SubscriptionMyResponse> getSubscriptions(SubscriptionStatus status) {
        List<Subscription> subscriptions = status != null
                ? subscriptionRepository.findByStatusOrderByCreatedAtDesc(status)
                : subscriptionRepository.findAllByOrderByCreatedAtDesc();

        return subscriptions.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SubscriptionStatusUpdateResponse updateStatus(UUID subscriptionId, SubscriptionStatusUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        subscription.updateStatus(request.getStatus());

        return SubscriptionStatusUpdateResponse.from(subscription);
    }

    // 전시 행에 쓰기 락을 걸어, 같은 전시의 해지 요청이 동시에 처리되지 않게 한다
    // 먼저 들어온 요청이 구독을 해지하면, 뒤의 요청은 해지된 구독을 보고 404를 받는다
    private Exhibition lockOwnedExhibitionOrThrow(UUID exhibitionId, UUID accountId) {
        Exhibition exhibition = exhibitionRepository.findForCodeUpdate(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }
        return exhibition;
    }

    // 비활성(is_active=false) 요금제는 신규 신청과 플랜 변경에서 막음
    private void requireActivePlan(Plan plan) {
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new SubscriptionException(SubscriptionErrorCode.PLAN_NOT_ACTIVE);
        }
    }

    private Exhibition findOwnedExhibitionOrThrow(UUID exhibitionId, UUID accountId) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }
        return exhibition;
    }

    private Subscription findCurrentSubscriptionOrThrow(UUID exhibitionId) {
        return subscriptionRepository
                .findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(exhibitionId, ONGOING_STATUSES)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
    }

    private PlanChangeType resolveChangeType(Plan previousPlan, Plan targetPlan) {
        Integer previousLimit = previousPlan.getMaxArtworkCount();
        Integer targetLimit = targetPlan.getMaxArtworkCount();

        if (Objects.equals(previousLimit, targetLimit)) {
            return PlanChangeType.SAME;
        }
        if (previousLimit == null) {
            return PlanChangeType.DOWNGRADE;
        }
        if (targetLimit == null) {
            return PlanChangeType.UPGRADE;
        }
        return targetLimit > previousLimit ? PlanChangeType.UPGRADE : PlanChangeType.DOWNGRADE;
    }

    private SubscriptionMyResponse toResponse(Subscription subscription) {
        return SubscriptionMyResponse.of(subscription, resolveExhibitionName(subscription.getExhibition()));
    }

    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .map(ExhibitionDetail::getTitle)
                .orElse(null);
    }
}
