package com.dolog.server.domain.exhibition.web.dto.response.basic;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionResolveResponse {

    private String uuid;

    public static ExhibitionResolveResponse of(UUID id) {
        return ExhibitionResolveResponse.builder()
                .uuid(id.toString())
                .build();
    }
}
