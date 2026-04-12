package com.dolog.server.domain.bts.web.dto.response;

import com.dolog.server.domain.bts.entity.Bts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BtsCreateResponse {
    private UUID btsId;
    private String title;

    public static BtsCreateResponse from(Bts bts) {
        return BtsCreateResponse.builder()
                .btsId(bts.getId())
                .title(bts.getTitle())
                .build();
    }
}
