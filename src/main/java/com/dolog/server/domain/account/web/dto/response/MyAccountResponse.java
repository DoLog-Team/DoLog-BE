package com.dolog.server.domain.account.web.dto.response;

import com.dolog.server.domain.account.entity.enums.AccountStatus;
import com.dolog.server.domain.account.entity.enums.Role;
import java.util.UUID;

public record MyAccountResponse(UUID accountId, String email, Role role, AccountStatus accountStatus,
                                UUID exhibitionId, UUID artistId) {
}
