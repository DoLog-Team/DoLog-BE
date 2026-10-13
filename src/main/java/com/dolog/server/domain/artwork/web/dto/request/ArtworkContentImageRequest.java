package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkContentImageRequest {

    @NotNull
    private MultipartFile imageFile;
}
