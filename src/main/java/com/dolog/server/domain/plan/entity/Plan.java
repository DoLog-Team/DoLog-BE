package com.dolog.server.domain.plan.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(length = 100)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 300)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "max_artwork_count")
    private Integer maxArtworkCount;

    @Column(name = "min_commitment_months")
    private Integer minCommitmentMonths;

    @Builder.Default
    @Column(name = "is_popular")
    private Boolean isPopular = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanPrice> prices = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanTargetSize> targetSizes = new ArrayList<>();
}
