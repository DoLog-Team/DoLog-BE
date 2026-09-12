package com.dolog.server.domain.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_token_account"))
    private Account account;

    @Column(nullable = false, length = 1024)
    private String token; // 발급된 Refresh Token 값

    @Builder
    public RefreshToken(Account account, String token) {
        this.account = account;
        this.token = token;
    }

    // 새로운 토큰이 발급되었을 때 값을 갱신하기 위한 메서드
    public void updateToken(String newToken) {
        this.token = newToken;
    }
}
