package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkImgUpdateRequest {
    private String imageUrl;
    private MultipartFile imageFile;
    private String description;
    private Integer orderIndex;
}
