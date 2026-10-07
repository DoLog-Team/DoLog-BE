package com.dolog.server.domain.artist.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistCreateRequest {

    @NotNull(message = "accountId는 필수입니다.")
    private UUID accountId;

    @NotBlank(message = "국문 성명은 필수입니다.")
    private String nameKo;

    private String nameEn;

    private String phone;
}
