package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.UserResult;

public interface SwingNavigator {

    void showAdminDashboard(UserResult user);

    void showProjectList(UserResult user);
}
