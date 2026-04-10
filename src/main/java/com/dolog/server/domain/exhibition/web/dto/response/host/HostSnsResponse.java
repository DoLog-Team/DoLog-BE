package com.dolog.server.domain.exhibition.web.dto.response.host;

import com.dolog.server.domain.exhibition.entity.HostSns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class HostSnsResponse {
    private UUID snsId;
    private String platformName;
    private String url;

    public static HostSnsResponse from(HostSns sns) {
        return HostSnsResponse.builder()
                .snsId(sns.getId())
                .platformName(sns.getPlatformName())
                .url(sns.getUrl())
                .build();
    }
}
