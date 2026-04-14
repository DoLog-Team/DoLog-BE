package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionCustomTheme;
import com.dolog.server.domain.exhibition.entity.enums.ThemeMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionCustomThemeResponse {

    @JsonProperty("exhibition_id")
    private String exhibitionId;

    @JsonProperty("splash_img")
    private String splashImg;

    @JsonProperty("theme_mode")
    private ThemeMode themeMode;

    @JsonProperty("btn_bg")
    private String btnBg;

    @JsonProperty("btn_text")
    private String btnText;

    @JsonProperty("cta_bg")
    private String ctaBg;

    @JsonProperty("cta_text")
    private String ctaText;

    public static ExhibitionCustomThemeResponse of(ExhibitionCustomTheme theme, String splashImg) {
        return ExhibitionCustomThemeResponse.builder()
                .exhibitionId(theme.getExhibition().getId().toString())
                .splashImg(splashImg)
                .themeMode(theme.getThemeMode())
                .btnBg(theme.getBtnBg())
                .btnText(theme.getBtnText())
                .ctaBg(theme.getCtaBg())
                .ctaText(theme.getCtaText())
                .build();
    }
}
