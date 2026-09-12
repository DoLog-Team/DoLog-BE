package com.dolog.server.domain.account.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionLoginRequest {
    @NotBlank
    private String entryCode;
}
