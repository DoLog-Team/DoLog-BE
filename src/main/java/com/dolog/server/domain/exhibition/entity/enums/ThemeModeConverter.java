package com.dolog.server.domain.exhibition.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ThemeModeConverter implements AttributeConverter<ThemeMode, String> {

    @Override
    public String convertToDatabaseColumn(ThemeMode attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public ThemeMode convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return ThemeMode.valueOf(dbData.toUpperCase());
    }
}
