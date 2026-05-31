package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.domain.Role;
import java.util.Objects;

record AccountCreateRequest(
        String loginId,
        String name,
        String password,
        Role role
) {

    AccountCreateRequest {
        role = Objects.requireNonNull(role, "role");
    }
}
