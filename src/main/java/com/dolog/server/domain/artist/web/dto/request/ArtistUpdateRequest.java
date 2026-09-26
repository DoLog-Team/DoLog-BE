package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class ArtistUpdateRequest {

    @Pattern(
            regexp = ".*\\S.*",
            message = "국문 성명은 공백일 수 없습니다."
    )
    private String nameKo;
    private String nameEn;
    private String phone;
}
