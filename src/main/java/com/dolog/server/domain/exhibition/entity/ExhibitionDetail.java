package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.exhibition.entity.enums.SortType;
import com.dolog.server.domain.exhibition.entity.enums.ThemeType;
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

    @Column(nullable = false, length = 255)
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

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "address_detail", columnDefinition = "TEXT")
    private String addressDetail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "sort_type", nullable = false)
    private SortType sortType = SortType.ABC;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_type")
    private ThemeType themeType;

    @Column(name = "splash_img")
    private String splashImg;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String copyright;

    public void updateSplashImg(String splashImg) {
        this.splashImg = splashImg;
    }

    public void updateBasicInfo(String title, String description, String exhibitionImg, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.exhibitionImg = exhibitionImg;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void update(String title, String description, String exhibitionImg, LocalDate startDate, LocalDate endDate,
                       String dateInfo, String address, String addressDetail, SortType sortType, ThemeType themeType, String splashImg,
                       String email, String copyright) {
        this.title = title;
        this.description = description;
        this.exhibitionImg = exhibitionImg;
        this.startDate = startDate;
        this.endDate = endDate;

        if (dateInfo != null) {
            this.dateInfo = dateInfo;
        }
        if (address != null) {
            this.address = address;
        }
        if (addressDetail != null) {
            this.addressDetail = addressDetail;
        }
        if (sortType != null) {
            this.sortType = sortType;
        }
        if (themeType != null) {
            this.themeType = themeType;
        }
        if (splashImg != null) {
            this.splashImg = splashImg;
        }
        if (email != null) {
            this.email = email;
        }
        if (copyright != null) {
            this.copyright = copyright;
        }
    }
}
