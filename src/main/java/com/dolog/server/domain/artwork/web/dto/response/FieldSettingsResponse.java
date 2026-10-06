package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;

public record FieldSettingsResponse(
        Required required,
        Hidden hidden
) {
    public record Required(boolean mainImg, boolean size, boolean materials, boolean locationMap) {}

    public record Hidden(boolean size, boolean materials, boolean locationMap,
                         boolean productionPeriod, boolean productionYear) {}

    public static FieldSettingsResponse from(ExhibitionFieldSettings settings) {
        return new FieldSettingsResponse(
                new Required(settings.isRequiredMainImg(), settings.isRequiredSize(),
                        settings.isRequiredMaterials(), settings.isRequiredLocationMap()),
                new Hidden(settings.isHiddenSize(), settings.isHiddenMaterials(), settings.isHiddenLocationMap(),
                        settings.isHiddenProductionPeriod(), settings.isHiddenProductionYear())
        );
    }
}
