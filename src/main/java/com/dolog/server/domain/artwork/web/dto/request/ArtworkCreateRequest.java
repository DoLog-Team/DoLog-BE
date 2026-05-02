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

    @NotBlank(message = "작품 카테고리는 필수입니다.")
    private String category;

    private String material;

    private String size;

    @NotBlank(message = "작품 상세 설명은 필수입니다.")
    private String description;

    private UUID zoneId;

    private String purchaseUrl;

    private String youtubeUrl;

    private Integer orderIndex;

    private String artistRole;

    @NotNull(message = "대표 이미지는 필수입니다.")
    private MultipartFile mainImageFile;

    private MultipartFile locationMapFile;
}