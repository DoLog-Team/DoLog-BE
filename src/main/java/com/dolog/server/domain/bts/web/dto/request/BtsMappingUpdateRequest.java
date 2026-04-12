package com.dolog.server.domain.bts.web.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String content;

    @NotNull
    private UUID artistProfileId;

    @NotNull
    private List<UUID> artworkIds;
}
