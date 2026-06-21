package com.dolog.server.domain.exhibition.web.dto.request.basic;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionUpdateRequest {

    private String univName;
    private String collegeName;
    private String deptName;
    private ExhibitionType exhibitionType;
    private String slug;
    private Boolean isPublic;
}
