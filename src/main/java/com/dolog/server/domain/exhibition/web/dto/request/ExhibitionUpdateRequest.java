package com.dolog.server.domain.exhibition.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionUpdateRequest {

    private String univName;
    private String deptName;
    private Boolean isPublic;
}
