package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExhibitionPublishResponse {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    public static ExhibitionPublishResponse from(Exhibition exhibition) {
        return ExhibitionPublishResponse.builder()
                .publishedAt(exhibition.getPublishedAt())
                .expiresAt(exhibition.getExpiresAt())
                .build();
    }
}
