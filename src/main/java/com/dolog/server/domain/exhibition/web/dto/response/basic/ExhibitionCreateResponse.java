package com.dolog.server.domain.exhibition.web.dto.response.basic;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExhibitionCreateResponse {

    private String id;
    private String slug;
    private String message;
    private String entryCode;
    private String artistJoinCode;
}
