package com.dolog.server.domain.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "terms_agreements")
public class TermsAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "age_14_or_over_confirmed", nullable = false)
    private boolean age14OrOverConfirmed;

    @Column(name = "service_terms_agreed", nullable = false)
    private boolean serviceTermsAgreed;

    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed;

    @Column(name = "privacy_3rd_party_agreed", nullable = false)
    private boolean privacy3rdPartyAgreed;

    @Column(name = "promotion_agreed")
    private Boolean promotionAgreed;

    @Column(name = "marketing_agreed")
    private Boolean marketingAgreed;

    @Column(name = "ad_email_agreed")
    private Boolean adEmailAgreed;

    @Column(name = "ad_kakao_agreed")
    private Boolean adKakaoAgreed;

    @Column(name = "ad_sms_agreed")
    private Boolean adSmsAgreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;
}
