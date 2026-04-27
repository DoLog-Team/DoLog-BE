package com.dolog.server.domain.exhibition.web.dto.response.host;

import com.dolog.server.domain.exhibition.entity.Host;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionHostDetailResponse {

    private UUID hostId;
    private String hostName;
    private String hostImageUrl;
    private String description;
    private String email;
    private List<HostSnsResponse> snsList;

    public static ExhibitionHostDetailResponse from(
            Host host,
            List<HostSnsResponse> snsList
    ) {
        return ExhibitionHostDetailResponse.builder()
                .hostId(host.getId())
                .hostName(host.getName())
                .hostImageUrl(host.getImg())
                .description(host.getDescription())
                .email(host.getEmail())
                .snsList(snsList)
                .build();
    }
}
