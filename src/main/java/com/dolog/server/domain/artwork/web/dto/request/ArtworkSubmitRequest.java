package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkSubmitRequest {

    @NotNull
    private UUID exhibitionId;

    @NotNull
    private UUID zoneId;

    private MultipartFile locationMapFile;
}
