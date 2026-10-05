package com.dolog.server.domain.plan.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionPolicyUpdateRequest {

    @NotNull(message = "유예기간(일)은 필수 입력 항목입니다.")
    @Min(value = 0, message = "유예기간은 0일 이상이어야 합니다.")
    private Integer cancelGraceDays;
}
