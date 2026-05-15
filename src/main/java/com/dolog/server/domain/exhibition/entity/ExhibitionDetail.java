package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.exhibition.entity.enums.SortType;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibition_details")
public class ExhibitionDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false, unique = true)
    private Exhibition exhibition;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "exhibition_img", nullable = false)
    private String exhibitionImg;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "date_info", columnDefinition = "TEXT")
    private String dateInfo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "sort_type", nullable = false)
    private SortType sortType = SortType.ABC;

    @Column(name = "splash_img")
    private String splashImg;

    @Column(name = "logo_img")
    private String logoImg;

    @Column(name = "og_image")
    private String ogImage;

    @Column(name = "favicon_img")
    private String faviconImg;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String copyright;

    public void updateSplashImg(String splashImg) {
        this.splashImg = splashImg;
    }

    public void updateBasicInfo(String title, String description, String exhibitionImg, LocalDate startDate, LocalDate endDate,
                                String dateInfo, String email, String copyright, String logoImg) {
        this.title = title;
        this.description = description;
        this.exhibitionImg = exhibitionImg;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateInfo = dateInfo;
        this.email = email;
        if (copyright != null) {
            this.copyright = copyright;
        }
        if (logoImg != null) {
            this.logoImg = logoImg;
        }
    }

    public void updateOgImage(String ogImage) {
        this.ogImage = ogImage;
    }

    public void updateFaviconImg(String faviconImg) {
        this.faviconImg = faviconImg;
    }
}
