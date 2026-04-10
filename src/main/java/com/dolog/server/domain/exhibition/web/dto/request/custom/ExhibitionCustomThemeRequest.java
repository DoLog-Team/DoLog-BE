package com.dolog.server.domain.exhibition.web.dto.request.custom;

import com.dolog.server.domain.exhibition.entity.enums.ThemeMode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionCustomThemeRequest {

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
}
