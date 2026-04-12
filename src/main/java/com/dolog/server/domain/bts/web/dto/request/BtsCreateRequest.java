package com.dolog.server.domain.bts.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BtsCreateRequest {
    private String title;
    private String contentUrl;      // 기존 externalLink
    private List<UUID> artworkIds;         // 이 ID 하나로 전시/작가 정보를 다 가져옵니다.
    private MultipartFile mainImg;  // 파일 업로드용 (Key: mainImg)
}
