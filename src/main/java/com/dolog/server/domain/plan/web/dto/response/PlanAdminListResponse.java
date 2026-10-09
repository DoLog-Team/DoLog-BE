package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Plan;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

// 두록 어드민 전용 요금제 목록. 비활성 요금제까지 포함하고 활성 여부(isActive)를 함께 내려준다.
@Getter
@Builder
public class PlanAdminListResponse {

    private List<Item> plans;

    @Getter
    @Builder
    public static class Item {

        @JsonUnwrapped
        private PlanResponse plan;

        private Boolean isActive;

        public static Item from(Plan plan) {
            return Item.builder()
                    .plan(PlanResponse.from(plan))
                    .isActive(plan.getIsActive())
                    .build();
        }
    }

    public static PlanAdminListResponse from(List<Plan> plans) {
        return PlanAdminListResponse.builder()
                .plans(plans.stream().map(Item::from).collect(Collectors.toList()))
                .build();
    }
}
