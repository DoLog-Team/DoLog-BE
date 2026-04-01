package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExhibitionCreateResponse {

    private String exhibitionId;
    private String message;
}
