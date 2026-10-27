package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ArtistUpdateRequest {

    @Pattern(
            regexp = "^$|.*\\S.*",
            message = "국문 성명은 공백일 수 없습니다."
    )
    @Size(max = 100)
    private String nameKo;
    @Size(max = 100)
    private String nameEn;
    @Size(max = 30)
    private String phone;
}
