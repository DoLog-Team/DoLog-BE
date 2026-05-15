package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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

    // TODO: JWT 연동 후 nullable = false 로 복구
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    @Column(name = "univ_name", length = 100, nullable = false)
    private String univName;

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

    public void updateBasicInfo(String univName, String deptName, ExhibitionType exhibitionType, String slug, Boolean isPublic) {
        if (univName != null) {
            this.univName = univName;
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
