package com.dolog.server.domain.bts.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BtsUpdateRequest {
    private String title;
    private String contentUrl;
    private UUID artistProfileId;
    private List<UUID> artworkIds;
}
