package com.dolog.server.domain.artist.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artists")
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private Account account; // v2 에서 nullable false (추후 account 연동)

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 30)
    private String phone;

    public void updateArtistInfo(String nameKo, String nameEn, String phone) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.phone = phone;
    }
}
