package com.dolog.server.domain.account.entity;

import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table( name = "accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accounts_social_identity",
                columnNames = {"social_provider", "social_provider_id"}
        )
)
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "social_provider", length = 50)
    private String socialProvider;

    @Column(name = "social_provider_id")
    private String socialProviderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;

    public void update(String password, Role role, String socialProvider, AccountStatus accountStatus) {
        if (password != null && !password.isBlank()) {
            this.password = password;
        }
        if (role != null) {
            this.role = role;
        }
        if (socialProvider != null) {
            this.socialProvider = socialProvider;
        }
        if (accountStatus != null) {
            this.accountStatus = accountStatus;
        }
    }
}
