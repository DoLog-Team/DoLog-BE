package com.dolog.server.domain.bts.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BtsCreateRequest {
    private String title;
    private String contentUrl;
    @NotEmpty(message = "작품 ID는 필수입니다.")
    private List<UUID> artworkIds;
}
