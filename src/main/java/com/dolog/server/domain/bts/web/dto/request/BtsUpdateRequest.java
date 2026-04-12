package com.dolog.server.domain.bts.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BtsUpdateRequest {
    private String title;
    private String contentUrl;
    private String mainImg;
    private UUID artistId;           // 작가 변경 시 사용
    private List<UUID> artworkIds;   // 연결할 작품 ID 리스트 (새로 덮어쓰기용)
}
