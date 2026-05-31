package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.Optional;

interface AccountDialogs {

    Optional<AccountCreateRequest> requestCreate(AccountManagementPanel parent);

    Optional<String> requestRename(AccountManagementPanel parent, UserResult selectedUser);

    Optional<Role> requestRole(AccountManagementPanel parent, UserResult selectedUser);

    boolean confirmActivation(AccountManagementPanel parent, UserResult selectedUser, boolean active);
}
