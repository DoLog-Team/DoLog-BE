package com.dolog.server.domain.plan.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionPolicyResponse {

    private Integer cancelGraceDays;

    public static SubscriptionPolicyResponse of(int cancelGraceDays) {
        return SubscriptionPolicyResponse.builder()
                .cancelGraceDays(cancelGraceDays)
                .build();
    }
}
