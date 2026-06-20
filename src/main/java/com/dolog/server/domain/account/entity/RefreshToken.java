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

    @Column(nullable = false, unique = true)
    private String email; // 사용자 식별자 (이메일)

    @Column(nullable = false)
    private String token; // 발급된 Refresh Token 값

    @Builder
    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    // 새로운 토큰이 발급되었을 때 값을 갱신하기 위한 메서드
    public void updateToken(String newToken) {
        this.token = newToken;
    }
}