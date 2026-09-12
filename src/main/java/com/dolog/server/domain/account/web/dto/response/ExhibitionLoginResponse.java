package com.dolog.server.domain.account.web.dto.response;

import com.dolog.server.domain.account.entity.enums.Role;
import java.util.UUID;

public record ExhibitionLoginResponse(UUID exhibitionId, boolean needsTermsAgreement,
        Role role, String accessToken, String refreshToken) {}
