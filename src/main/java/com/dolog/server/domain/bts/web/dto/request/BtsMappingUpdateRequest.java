package com.dolog.server.domain.bts.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BtsMappingUpdateRequest {

    @NotBlank
    private String title;

    private String linkLabel;

    private String linkUrl;

    @NotNull
    private UUID artistProfileId;

    @NotEmpty
    private List<@NotNull UUID> artworkIds;
}
