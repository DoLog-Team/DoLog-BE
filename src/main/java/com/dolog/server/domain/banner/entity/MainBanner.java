package com.dolog.server.domain.banner.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "main_banners")
public class MainBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "is_visible")
    private Boolean isVisible;
}