package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 두록 어드민용 코드 재조회. 발급 API 응답과 같은 필드 이름을 쓴다.
@Getter
@Builder
public class ExhibitionCodesResponse {

    private String entryCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime entryCodeExpiresAt;

    private boolean entryCodeExpired;

    private String artistJoinCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime artistJoinCodeExpiresAt;

    private boolean artistJoinCodeExpired;

    public static ExhibitionCodesResponse from(Exhibition exhibition) {
        LocalDateTime now = LocalDateTime.now();
        return ExhibitionCodesResponse.builder()
                .entryCode(exhibition.getEntryCode())
                .entryCodeExpiresAt(exhibition.getEntryCodeExpiresAt())
                .entryCodeExpired(isExpired(exhibition.getEntryCodeExpiresAt(), now))
                .artistJoinCode(exhibition.getArtistJoinCode())
                .artistJoinCodeExpiresAt(exhibition.getArtistJoinCodeExpiresAt())
                .artistJoinCodeExpired(isExpired(exhibition.getArtistJoinCodeExpiresAt(), now))
                .build();
    }

    // expiresAt이 null이면 만료 없음(= 만료 아님). 엔티티의 requireXxxCodeValid()와 같은 규칙.
    private static boolean isExpired(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
