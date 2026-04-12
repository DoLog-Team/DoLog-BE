package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtworkArtistMappingResponse {

    private Long mappingId;
    private UUID artistProfileId; // artistId에서 artistProfileId로 명칭 변경
    private String artistName;    // 작가 이름 추가
    private String artistRole;

    // 작품 정보
    private ArtworkInfo artwork;

    // 전시회 정보
    private ExhibitionInfo exhibition;

    /**
     * 작품 정보 DTO
     */
    @Getter
    @Builder
    public static class ArtworkInfo {
        private UUID id;
        private String title;
        private List<ArtistMemberInfo> artists;
    }

    @Getter
    @Builder
    public static class ArtistMemberInfo {
        private UUID artistProfileId;
        private String name;
        private String role;
    }

    /**
     * 전시회 정보 DTO
     */
    @Getter
    @Builder
    public static class ExhibitionInfo {
        private UUID id;
        private String univ;
        private String title;
    }

    // [등록/수정 공통] 엔티티를 받아 전체 정보를 구성합니다.
    public static ArtworkArtistMappingResponse of(ArtworkArtistMap map) {
        Exhibition exhibition = map.getArtwork().getExhibition();

        // ExhibitionDetail을 가져오되, null일 경우를 대비 (NPE 방지)
        ExhibitionDetail detail = exhibition.getExhibitionDetail();
        String exhibitionTitle = (detail != null) ? detail.getTitle() : "제목 없음";

        // 1. 해당 작품에 연결된 모든 작가 매핑 리스트 추출
        // 엔티티 필드명인 artworkArtistMaps를 사용합니다.
        List<ArtistMemberInfo> artistList = map.getArtwork().getArtworkArtistMaps().stream()
                .map(m -> ArtistMemberInfo.builder()
                        .artistProfileId(m.getArtistProfile().getId())
                        .name(m.getArtistProfile().getNameKo())
                        .role(m.getArtistRole())
                        .build())
                .collect(Collectors.toList());

        return ArtworkArtistMappingResponse.builder()
                .mappingId(map.getId())
                // 현재 작업 중인 작가 정보 (루트 레벨)
                .artistProfileId(map.getArtistProfile().getId())
                .artistName(map.getArtistProfile().getNameKo())
                .artistRole(map.getArtistRole())
                // 작품 및 소속된 전체 작가 리스트
                .artwork(ArtworkInfo.builder()
                        .id(map.getArtwork().getId())
                        .title(map.getArtwork().getTitle())
                        .artists(artistList)
                        .build())
                // 전시 정보
                .exhibition(ExhibitionInfo.builder()
                        .id(exhibition.getId())
                        .univ(exhibition.getUnivName())
                        .title(exhibitionTitle)
                        .build())
                .build();
    }

    // 기존의 from 메서드는 of로 대체하여 풍부한 정보를 제공하는 것을 권장합니다.
    @Deprecated
    public static ArtworkArtistMappingResponse from(Long id) {
        return ArtworkArtistMappingResponse.builder()
                .mappingId(id)
                .build();
    }
}