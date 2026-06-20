package com.dolog.server.domain.artist.web.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Getter
@Setter
public class ArtistProfileCreateRequest {
    private UUID exhibitionId;
    private String artistId;

    private String nameKo;
    private String nameEn;
    private String bio;
    private String email;
    private MultipartFile profileImg;
    private Boolean isDeleteImg = false;
}
