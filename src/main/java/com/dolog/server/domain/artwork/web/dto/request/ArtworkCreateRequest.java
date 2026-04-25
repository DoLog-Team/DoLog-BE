package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkCreateRequest {
    @NotNull(message = "작가 프로필 ID는 필수입니다.")
    private UUID artistProfileId;

    @NotBlank(message = "작품 제목은 필수입니다.")
    private String title;

    private String category;
    private String material;
    private String size;
    private String description;
    private UUID zoneId;
    private String purchaseUrl;
    private Integer orderIndex;
    private String artistRole;

    private MultipartFile mainImageFile;
    private MultipartFile locationMapFile;
}