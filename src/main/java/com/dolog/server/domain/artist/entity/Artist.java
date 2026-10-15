package com.dolog.server.domain.artist.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artists")
public class Artist extends BaseEntity {

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private Account account; // v2 에서 nullable false (추후 account 연동)

    @Column(name = "name_ko", length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 30)
    private String phone;


    @OneToOne(mappedBy = "artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private ArtistProfile profile;

    public void updateArtistInfo(String nameKo, String nameEn, String phone) {
        if (nameKo != null) this.nameKo = nameKo;
        if (nameEn != null) this.nameEn = nameEn;
        if (phone != null) this.phone = phone;
    }

    public void softDelete(LocalDateTime deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
        }
    }

    public void restore(String nameKo, String nameEn, String phone) {
        this.deletedAt = null;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.phone = phone;
    }
}
