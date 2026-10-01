package com.dolog.server.domain.plan.service;

import com.dolog.server.domain.plan.entity.Plan;
import com.dolog.server.domain.plan.entity.PlanPrice;
import com.dolog.server.domain.plan.entity.PlanTargetSize;
import com.dolog.server.domain.plan.exception.PlanErrorCode;
import com.dolog.server.domain.plan.exception.PlanException;
import com.dolog.server.domain.plan.repository.PlanRepository;
import com.dolog.server.domain.plan.web.dto.request.PlanCreateRequest;
import com.dolog.server.domain.plan.web.dto.request.PlanUpdateRequest;
import com.dolog.server.domain.plan.web.dto.response.PlanCreateResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanListResponse;
import com.dolog.server.domain.plan.web.dto.response.PlanUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    @Override
    @Transactional
    public PlanCreateResponse createPlan(PlanCreateRequest request) {
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxArtworkCount(request.getMaxArtworkCount())
                .minCommitmentMonths(request.getMinCommitmentMonths())
                .isPopular(request.getIsPopular() != null ? request.getIsPopular() : false)
                .displayOrder(request.getDisplayOrder())
                .build();

        request.getTargetSizes().forEach(targetSize ->
                plan.getTargetSizes().add(
                        PlanTargetSize.builder()
                                .plan(plan)
                                .targetSize(targetSize)
                                .build()
                )
        );

        request.getPrices().forEach(priceRequest ->
                plan.getPrices().add(
                        PlanPrice.builder()
                                .plan(plan)
                                .billingCycle(priceRequest.getBillingCycle())
                                .price(priceRequest.getPrice())
                                .discountRate(priceRequest.getDiscountRate())
                                .months(priceRequest.getMonths())
                                .build()
                )
        );

        Plan saved = planRepository.save(plan);

        return PlanCreateResponse.from(saved);
    }

    @Override
    public PlanListResponse getPlans() {
        return PlanListResponse.from(planRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    @Override
    @Transactional
    public PlanUpdateResponse updatePlan(UUID planId, PlanUpdateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanException(PlanErrorCode.PLAN_NOT_FOUND));

        plan.updateBasicInfo(
                request.getName(),
                request.getDescription(),
                request.getMaxArtworkCount(),
                request.getMinCommitmentMonths(),
                request.getIsPopular(),
                request.getDisplayOrder()
        );

        if (request.getTargetSizes() != null) {
            plan.replaceTargetSizes(request.getTargetSizes());
        }

        if (request.getPrices() != null) {
            request.getPrices().forEach(priceRequest ->
                    plan.upsertPrice(
                            priceRequest.getBillingCycle(),
                            priceRequest.getPrice(),
                            priceRequest.getDiscountRate(),
                            priceRequest.getMonths()
                    )
            );
        }

        return PlanUpdateResponse.from(plan);
    }
}
