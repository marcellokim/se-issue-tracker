package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.List;

interface AccountManagementView {

    void showUsers(List<UserResult> users);

    void showMessage(String message, boolean error);
}
