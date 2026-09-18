package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.dolog.server.global.entity.BaseEntity;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibitions")
public class Exhibition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "entry_code", length = 8, unique = true)
    private String entryCode;

    @Column(name = "artist_join_code", length = 8, unique = true)
    private String artistJoinCode;

    @Column(name = "entry_code_expires_at")
    private LocalDateTime entryCodeExpiresAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public void reissueEntryCode(String code, LocalDateTime codeExpiresAt) {
        this.entryCode = code;
        this.entryCodeExpiresAt = codeExpiresAt;
    }

    public void requireAvailable() {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_EXPIRED);
        }
    }

    public void requireEntryCodeValid() {
        if (entryCodeExpiresAt != null && !entryCodeExpiresAt.isAfter(LocalDateTime.now())) {
            throw new ExhibitionException(ExhibitionErrorCode.ENTRY_CODE_EXPIRED);
        }
    }

    @Column(name = "univ_name", length = 100, nullable = false)
    private String univName;

    @Column(name = "college_name", length = 100)
    private String collegeName;

    @Column(name = "dept_name", length = 100, nullable = false)
    private String deptName;

    @Enumerated(EnumType.STRING)
    @Column(name = "exhibition_type", length = 50)
    private ExhibitionType exhibitionType;

    @Column(name = "slug", length = 100, unique = true, nullable = false)
    private String slug;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @OneToOne(mappedBy = "exhibition", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private ExhibitionMap exhibitionMap;

    @OneToOne(mappedBy = "exhibition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ExhibitionDetail exhibitionDetail;

    public void updateBasicInfo(String univName, String collegeName, String deptName, ExhibitionType exhibitionType, String slug, Boolean isPublic) {
        if (univName != null) {
            this.univName = univName;
        }
        if (collegeName != null) {
            this.collegeName = collegeName;
        }
        if (deptName != null) {
            this.deptName = deptName;
        }
        if (exhibitionType != null) {
            this.exhibitionType = exhibitionType;
        }
        if (slug != null) {
            this.slug = slug;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

}
