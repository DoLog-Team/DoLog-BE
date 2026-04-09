package com.dolog.server.domain.exhibition.web.dto.response;

import com.dolog.server.domain.exhibition.entity.Host;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionHostResponse {
    private UUID hostId;
    private String hostName;
    private String hostImageUrl;
    private String description;

    public static ExhibitionHostResponse from(Host host) {
        return ExhibitionHostResponse.builder()
                .hostId(host.getId())
                .hostName(host.getName())
                .hostImageUrl(host.getImg())
                .description(host.getDescription())
                .build();
    }
}
