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
@Table(name = "hosts")
public class Host extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @Column(nullable = false, length = 100)
    private String name;

    private String img;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String email;

    public void update(String name, String img, String description, String email) {
        if (name != null) this.name = name;
        if (img != null) this.img = img;
        if (description != null) this.description = description;
        if (email != null) this.email = email;
    }
}
