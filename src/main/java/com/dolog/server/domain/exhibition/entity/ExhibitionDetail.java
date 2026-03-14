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

    @Column(nullable = false)
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

    @Column(name = "address_info", columnDefinition = "TEXT")
    private String addressInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_type", nullable = false)
    private SortType sortType;

    public void updateDetails(String title, String description, String exhibitionImg, LocalDate startDate, LocalDate endDate, String address) {
        this.title = title;
        this.description = description;
        this.exhibitionImg = exhibitionImg;
        this.startDate = startDate;
        this.endDate = endDate;
        this.address = address;
    }

    public void updateSortType(SortType sortType) {
        this.sortType = sortType;
    }
}
