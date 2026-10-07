package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.exhibition.entity.Exhibition;
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
    private UUID artistId;
    private UUID artistProfileId;
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
        private UUID artistId;
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

    // [등록/수정 공통] 프로필이 없는 작가(전시 프로필 미연결)와 출품 전 작품도 응답할 수 있어야 한다.
    public static ArtworkArtistMappingResponse of(ArtworkArtistMap map) {
        Exhibition exhibition = map.getArtwork().getExhibition();

        List<ArtistMemberInfo> artistList = map.getArtwork().getArtworkArtistMaps().stream()
                .map(m -> ArtistMemberInfo.builder()
                        .artistId(m.getArtist().getId())
                        .artistProfileId(m.getArtistProfile() != null ? m.getArtistProfile().getId() : null)
                        .name(nameOf(m))
                        .role(m.getArtistRole())
                        .build())
                .collect(Collectors.toList());

        return ArtworkArtistMappingResponse.builder()
                .mappingId(map.getId())
                .artistId(map.getArtist().getId())
                .artistProfileId(map.getArtistProfile() != null ? map.getArtistProfile().getId() : null)
                .artistName(nameOf(map))
                .artistRole(map.getArtistRole())
                .artwork(ArtworkInfo.builder()
                        .id(map.getArtwork().getId())
                        .title(map.getArtwork().getTitle())
                        .artists(artistList)
                        .build())
                .exhibition(exhibition == null ? null : ExhibitionInfo.builder()
                        .id(exhibition.getId())
                        .univ(exhibition.getUnivName())
                        .title(exhibition.getExhibitionDetail() != null
                                ? exhibition.getExhibitionDetail().getTitle() : "제목 없음")
                        .build())
                .build();
    }

    private static String nameOf(ArtworkArtistMap map) {
        return map.getArtistProfile() != null ? map.getArtistProfile().getNameKo() : map.getArtist().getNameKo();
    }

    // 기존의 from 메서드는 of로 대체하여 풍부한 정보를 제공하는 것을 권장합니다.
    @Deprecated
    public static ArtworkArtistMappingResponse from(Long id) {
        return ArtworkArtistMappingResponse.builder()
                .mappingId(id)
                .build();
    }
}