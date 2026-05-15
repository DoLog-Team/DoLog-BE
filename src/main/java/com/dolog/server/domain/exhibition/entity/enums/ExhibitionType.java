package com.dolog.server.domain.exhibition.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExhibitionType {
    GRADUATION,   // 졸업 전시
    ASSIGNMENT;   // 과제전

    @JsonCreator
    public static ExhibitionType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ExhibitionType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
