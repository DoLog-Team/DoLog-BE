package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "partner_members")
public class PartnerMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    private String name;

    @Column(length = 100)
    private String role;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String imageUrl;

    public void update(String name, String email) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
    }
}
