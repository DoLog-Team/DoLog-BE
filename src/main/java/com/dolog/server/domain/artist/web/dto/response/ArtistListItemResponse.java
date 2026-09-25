package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ArtistListItemResponse {

    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String phone;
    private String accountEmail;
    private LocalDateTime createdAt;

    public static ArtistListItemResponse from(Artist artist) {
        return ArtistListItemResponse.builder()
                .artistId(artist.getId())
                .nameKo(artist.getNameKo())
                .nameEn(artist.getNameEn())
                .phone(artist.getPhone())
                .accountEmail(
                        artist.getAccount() != null
                                ? artist.getAccount().getEmail()
                                : null
                )
                .createdAt(artist.getCreatedAt())
                .build();
    }
}
