package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FieldSettingsRequest(
        @NotNull @Valid Required required,
        @NotNull @Valid Hidden hidden
) {
    public record Required(
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean mainImg,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean size,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean materials,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean locationMap
    ) {}

    public record Hidden(
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean size,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean materials,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean locationMap,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean productionPeriod,
            @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean productionYear
    ) {}

    // 작가에게 채우라고 하면서 노출은 안 하는 모순을 막는다. 대표 이미지는 숨김, 제작 기간은 필수 설정이 없다.
    public boolean hasRequiredAndHiddenConflict() {
        return (required.size() && hidden.size())
                || (required.materials() && hidden.materials())
                || (required.locationMap() && hidden.locationMap());
    }
}
