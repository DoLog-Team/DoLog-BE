package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.entity.enums.Role;
import com.dolog.server.domain.account.web.dto.request.AdminCreateRequest;
import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.account.web.dto.request.ChangePasswordRequest;

import com.dolog.server.domain.account.web.dto.response.MyAccountResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    /**
     * 새로운 관리자를 생성
     * 슈퍼어드민만 호출 가능
     */
    Account createAdmin(AdminCreateRequest request);
    MyAccountResponse getMyAccount(UUID accountId);
    List<Account> getAccounts(Role role);
    Account changePassword(UUID accountId, ChangePasswordRequest request);
}