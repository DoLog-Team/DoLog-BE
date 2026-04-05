package com.dolog.server.domain.account.web.dto.response;

import com.dolog.server.domain.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private String id;              // UUID → String으로 변환
    private String email;
    private String role;
    private String accountStatus;   // enum → String

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .role(account.getRole().name())
                .accountStatus(account.getAccountStatus().name())
                .build();
    }
}
