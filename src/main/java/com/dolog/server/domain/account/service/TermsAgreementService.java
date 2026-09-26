package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.TermsAgreement;
import com.dolog.server.domain.account.repository.AccountRepository;
import com.dolog.server.domain.account.repository.TermsAgreementRepository;
import com.dolog.server.domain.account.web.dto.request.TermsAgreementRequest;
import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.exception.jwt.JwtInvalidException;
import com.dolog.server.domain.account.exception.AccountErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TermsAgreementService {
    private final TermsAgreementRepository agreements;
    private final AccountRepository accounts;
    private final String currentVersion;

    public TermsAgreementService(TermsAgreementRepository agreements, AccountRepository accounts,
                                 @Value("${terms.current-version:}") String currentVersion) {
        this.agreements = agreements;
        this.accounts = accounts;
        this.currentVersion = currentVersion;
    }

    @Transactional(readOnly = true)
    public boolean needsAgreement(UUID accountId) {
        return !agreements.hasRequiredAgreement(accountId, requiredVersion());
    }

    @Transactional
    public LocalDateTime agree(UUID accountId, TermsAgreementRequest request) {
        String version = requiredVersion();
        if (!version.equals(request.termsVersion())) {
            throw new BaseException(AccountErrorCode.TERMS_VERSION_MISMATCH);
        }
        // 광고 수신 동의는 마케팅 목적 수집 동의가 전제다.
        if (!Boolean.TRUE.equals(request.age14OrOverConfirmed())
                || !Boolean.TRUE.equals(request.serviceTermsAgreed()) || !Boolean.TRUE.equals(request.privacyAgreed())
                || (Boolean.TRUE.equals(request.adReceiveAgreed()) && !Boolean.TRUE.equals(request.marketingAgreed()))) {
            throw new BaseException(AccountErrorCode.TERMS_REQUIRED_MISSING);
        }
        // 역할·활성 상태는 컨트롤러 @PreAuthorize와 JWT 필터가 이미 검증한다.
        var account = accounts.findById(accountId).orElseThrow(JwtInvalidException::new);
        LocalDateTime agreedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        agreements.save(TermsAgreement.builder()
                .account(account).termsVersion(version)
                .age14OrOverConfirmed(true).serviceTermsAgreed(true).privacyAgreed(true)
                .privacy3rdPartyAgreed(false)
                .promotionAgreed(request.promotionAgreed()).marketingAgreed(request.marketingAgreed())
                .adEmailAgreed(request.adReceiveAgreed()).adKakaoAgreed(request.adReceiveAgreed())
                .adSmsAgreed(request.adReceiveAgreed()).agreedAt(agreedAt).build());
        return agreedAt;
    }

    private String requiredVersion() {
        if (currentVersion == null || currentVersion.isBlank()) {
            throw new BaseException(AccountErrorCode.TERMS_NOT_CONFIGURED);
        }
        return currentVersion;
    }
}
