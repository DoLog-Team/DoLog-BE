package com.dolog.server.domain.account.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptionalTermsAgreementRequest(
        @NotBlank String termsVersion,
        @NotNull Boolean promotionAgreed,
        @NotNull Boolean marketingAgreed,
        @NotNull Boolean adReceiveAgreed
) {
}
