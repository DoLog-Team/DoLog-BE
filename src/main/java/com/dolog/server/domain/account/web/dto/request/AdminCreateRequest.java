package com.dolog.server.domain.account.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminCreateRequest {
    private String email;
    private String password;
}