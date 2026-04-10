package com.dolog.server.domain.exhibition.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ThemeMode {
    LIGHT, DARK;

    @JsonCreator
    public static ThemeMode from(String value) {
        return ThemeMode.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
