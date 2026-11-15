package com.dolog.server.domain.account.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionLoginRequest {
    @NotBlank
    @Size(max = 500)
    private String entryCode;
}
