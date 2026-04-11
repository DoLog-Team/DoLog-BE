package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 응답 JSON에서 제외합니다.
public class ArtworkArtistMappingResponse {

    private Long mappingId; // 등록 시 사용 (Long 타입)
    private UUID artistId;  // 수정 시 사용 (UUID 타입)
    private String artistRole; // 수정 시 사용

    // [등록용] mapping_id만 담아서 반환
    public static ArtworkArtistMappingResponse from(Long id) {
        return ArtworkArtistMappingResponse.builder()
                .mappingId(id)
                .build();
    }

    // 수정용: 엔티티(map)를 통째로 받아서 필요한 정보만 추출합니다.
    public static ArtworkArtistMappingResponse of(ArtworkArtistMap map) {
        return ArtworkArtistMappingResponse.builder()
                .artistId(map.getArtist().getId())      // 작가 UUID 추출
                .artistRole(map.getArtistRole())        // 수정된 역할 추출
                .build();
    }
}