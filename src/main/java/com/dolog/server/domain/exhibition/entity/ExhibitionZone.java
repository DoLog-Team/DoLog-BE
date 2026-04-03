package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibition_zones")
public class ExhibitionZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}
