package com.dolog.server.domain.account.web.dto.response;

import com.dolog.server.domain.account.entity.TermsAgreement;

import java.time.LocalDateTime;

public record TermsAgreementDetailResponse(LocalDateTime agreedAt, Terms terms) {
    public static TermsAgreementDetailResponse from(TermsAgreement agreement) {
        return new TermsAgreementDetailResponse(agreement.getAgreedAt(), new Terms(
                agreement.getTermsVersion(), agreement.isAge14OrOverConfirmed(),
                agreement.isServiceTermsAgreed(), agreement.isPrivacyAgreed(),
                agreement.isPrivacy3rdPartyAgreed(), agreement.getPromotionAgreed(),
                agreement.getMarketingAgreed(), agreement.getAdEmailAgreed(),
                agreement.getAdKakaoAgreed(), agreement.getAdSmsAgreed()));
    }

    public record Terms(String termsVersion, boolean age14OrOverConfirmed,
                        boolean serviceTermsAgreed, boolean privacyAgreed,
                        boolean privacy3rdPartyAgreed, Boolean promotionAgreed,
                        Boolean marketingAgreed, Boolean adEmailAgreed,
                        Boolean adKakaoAgreed, Boolean adSmsAgreed) {
    }
}
