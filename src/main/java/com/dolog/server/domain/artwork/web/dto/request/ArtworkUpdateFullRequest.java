package com.dolog.server.domain.artwork.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ArtworkUpdateFullRequest {
    private String title;
    private String description;
    private String category;
    private UUID zoneId;
    private String purchaseUrl;
    private List<UUID> artistIds;
    private List<ImageUpdateDto> images;

    @Getter
    @NoArgsConstructor
    public static class ImageUpdateDto {
        private UUID id;             // 선택 (기존 이미지 수정 시 필요, 신규 추가 시 null)
        private String imageUrl;     // 기존 이미지 유지 시 사용
        private MultipartFile imageFile; // 새 파일 업로드 시 사용
        private String description;  // 선택
        private Integer orderIndex;  // 필수
    }
}