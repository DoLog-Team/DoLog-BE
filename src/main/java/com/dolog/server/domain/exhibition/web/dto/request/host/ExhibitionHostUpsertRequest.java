package com.dolog.server.domain.exhibition.web.dto.request.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExhibitionHostUpsertRequest {
    private String host_name;
    private String host_image_url;
    private String description;
}
