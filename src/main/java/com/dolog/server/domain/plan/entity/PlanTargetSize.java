package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.plan.entity.enums.TargetSize;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "plan_target_sizes",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_target_sizes_size", columnNames = {"plan_id", "target_size"})
)
public class PlanTargetSize {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_size", nullable = false, length = 20)
    private TargetSize targetSize;
}
