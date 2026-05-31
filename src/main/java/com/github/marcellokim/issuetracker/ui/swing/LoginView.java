package com.github.marcellokim.issuetracker.ui.swing;

public interface LoginView {

    String loginId();

    String password();

    void setLoginEnabled(boolean enabled);

    void showMessage(String message, boolean error);

    void clearPassword();
}
