package com.dolog.server.domain.artwork.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkUpdateFullRequest {
    private String title;
    private String description;
    private String category;

    private UUID zoneId;
    private Integer prevOrder;
    private Integer nextOrder;

    private String youtubeUrl;
    private String purchaseUrl;
    private List<UUID> artistIds = new ArrayList<>();
    private Map<UUID, String> artistRoles = new HashMap<>(); // artistId → role (없으면 기존 역할 유지)
    private List<ImageUpdateDto> images = new ArrayList<>();


    @Getter
    @Setter
    @NoArgsConstructor
    public static class ImageUpdateDto {
        private UUID id;             // 선택 (기존 이미지 수정 시 필요, 신규 추가 시 null)
        private String imageUrl;     // 기존 이미지 유지 시 사용
        private MultipartFile imageFile; // 새 파일 업로드 시 사용
        private String description;  // 선택
        private Integer orderIndex;  // 필수
    }
}