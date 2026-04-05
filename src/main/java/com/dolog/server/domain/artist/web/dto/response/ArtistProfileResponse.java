package com.dolog.server.domain.artist.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfileResponse {
    private UUID profileId;
    private String nameKo;
    private String nameEn;
    private String bio;
    private String email;
    private String profileImg; // 파일 경로 또는 URL
}