package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.PlanPrice;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.exception.PlanErrorCode;
import com.dolog.server.domain.plan.exception.PlanException;
import com.dolog.server.domain.plan.exception.SubscriptionErrorCode;
import com.dolog.server.domain.plan.exception.SubscriptionException;
import com.dolog.server.domain.plan.repository.PlanRepository;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import com.dolog.server.domain.plan.web.dto.request.SubscriptionCreateRequest;
import com.dolog.server.domain.plan.web.dto.response.SubscriptionCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional
    public SubscriptionCreateResponse createSubscription(UUID exhibitionId, SubscriptionCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        boolean alreadySubscribed = subscriptionRepository.existsByExhibitionIdAndStatusIn(
                exhibitionId, List.of(SubscriptionStatus.PENDING_PAYMENT, SubscriptionStatus.ACTIVE)
        );
        if (alreadySubscribed) {
            throw new SubscriptionException(SubscriptionErrorCode.ALREADY_SUBSCRIBED);
        }

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

        PlanPrice planPrice = plan.findPriceByCycle(request.getBillingCycle())
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.PLAN_PRICE_NOT_FOUND));

        Subscription subscription = Subscription.builder()
                .exhibition(exhibition)
                .plan(plan)
                .billingCycle(request.getBillingCycle())
                .months(planPrice.getMonths())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        return SubscriptionCreateResponse.of(saved, planPrice.getPrice());
    }
}
