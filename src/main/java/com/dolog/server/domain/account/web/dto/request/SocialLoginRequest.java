package com.dolog.server.domain.account.web.dto.request;

import com.dolog.server.domain.account.entity.enums.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialLoginRequest(
        @NotNull SocialProvider provider,
        @NotBlank @Size(max = 2048) String authorizationCode,
        @NotBlank @Size(max = 2048) String redirectUri
) {
}
