package com.dolog.server.domain.artist.web.dto;

import com.dolog.server.domain.artist.entity.Artist;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ArtistResponse {

    private UUID id;
    private String nameKo;
    private String nameEn;
    private String phone;

    public static ArtistResponse from(Artist artist) {
        return ArtistResponse.builder()
                .id(artist.getId())
                .nameKo(artist.getNameKo())
                .nameEn(artist.getNameEn())
                //.email(artist.getAccount().getEmail()) // 카카오 로그인 시 이메일 연동
                .phone(artist.getPhone())
                .build();
    }
}
