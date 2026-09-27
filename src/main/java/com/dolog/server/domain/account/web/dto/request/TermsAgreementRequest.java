package com.dolog.server.domain.account.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TermsAgreementRequest(
        @NotBlank String termsVersion,
        @NotNull Boolean age14OrOverConfirmed,
        @NotNull Boolean serviceTermsAgreed,
        @NotNull Boolean privacyAgreed,
        @NotNull Boolean promotionAgreed,
        @NotNull Boolean marketingAgreed,
        @NotNull Boolean adReceiveAgreed
) {
}
