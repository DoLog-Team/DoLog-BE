package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionDetailUpsertResponse {

    private UUID exhibitionId;

    private String message;
}
