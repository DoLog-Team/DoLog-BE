package com.dolog.server.domain.account.repository;

import com.dolog.server.domain.account.entity.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, UUID> {
    @Query("select count(t) > 0 from TermsAgreement t where t.account.id = :accountId "
            + "and t.termsVersion = :termsVersion and t.age14OrOverConfirmed = true "
            + "and t.serviceTermsAgreed = true and t.privacyAgreed = true")
    boolean hasRequiredAgreement(@Param("accountId") UUID accountId, @Param("termsVersion") String termsVersion);
}
