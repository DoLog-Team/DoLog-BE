package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.exhibition.entity.enums.ThemeMode;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibition_custom_themes")
public class ExhibitionCustomTheme extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false, unique = true)
    private Exhibition exhibition;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_mode", nullable = false)
    private ThemeMode themeMode;

    @Column(name = "btn_bg", length = 20)
    private String btnBg;

    @Column(name = "btn_text", length = 20)
    private String btnText;

    @Column(name = "cta_bg", length = 20)
    private String ctaBg;

    @Column(name = "cta_text", length = 20)
    private String ctaText;

    public void update(ThemeMode themeMode, String btnBg, String btnText, String ctaBg, String ctaText) {
        if (themeMode != null) this.themeMode = themeMode;
        if (btnBg != null) this.btnBg = btnBg;
        if (btnText != null) this.btnText = btnText;
        if (ctaBg != null) this.ctaBg = ctaBg;
        if (ctaText != null) this.ctaText = ctaText;
    }
}
