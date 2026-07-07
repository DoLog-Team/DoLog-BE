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

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "exhibition_img")
    private String exhibitionImg;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
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

    @Column(name = "og_title", length = 255)
    private String ogTitle;

    @Column(name = "og_description", columnDefinition = "TEXT")
    private String ogDescription;

    public void updateSplashImg(String splashImg) {
        this.splashImg = splashImg;
    }

    public void updateBasicInfo(String title, String description, String exhibitionImg, LocalDate startDate, LocalDate endDate,
                                String dateInfo, String email, String copyright, String logoImg) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (exhibitionImg != null) this.exhibitionImg = exhibitionImg;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (dateInfo != null) this.dateInfo = dateInfo;
        if (email != null) this.email = email;
        if (copyright != null) this.copyright = copyright;
        if (logoImg != null) this.logoImg = logoImg;
    }

    public void updateOgImage(String ogImage) {
        this.ogImage = ogImage;
    }

    public void updateFaviconImg(String faviconImg) {
        this.faviconImg = faviconImg;
    }

    public void updateOgMeta(String ogTitle, String ogDescription, String ogImage, String faviconImg) {
        if (ogTitle != null) this.ogTitle = ogTitle;
        if (ogDescription != null) this.ogDescription = ogDescription;
        if (ogImage != null) this.ogImage = ogImage;
        if (faviconImg != null) this.faviconImg = faviconImg;
    }
}
