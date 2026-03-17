package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.exhibition.entity.enums.ThemeType;
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
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "univ_name", length = 100)
    private String univName;

    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    public void updateBasicInfo(String univName, String deptName, Boolean isPublic) {
        if (univName != null) {
            this.univName = univName;
        }
        if (deptName != null) {
            this.deptName = deptName;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }
}
