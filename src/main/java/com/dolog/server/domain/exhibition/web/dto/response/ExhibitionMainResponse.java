package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExhibitionMainResponse {

    private List<ExhibitionListItemResponse> mainExhibitions;
}
