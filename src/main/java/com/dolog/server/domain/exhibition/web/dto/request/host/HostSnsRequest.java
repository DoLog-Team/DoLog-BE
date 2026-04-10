package com.dolog.server.domain.exhibition.web.dto.request.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HostSnsRequest {
    private String platformName;
    private String url;
}
