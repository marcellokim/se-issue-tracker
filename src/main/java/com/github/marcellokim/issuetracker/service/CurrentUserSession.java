package com.github.marcellokim.issuetracker.service;

import java.util.Optional;

public interface CurrentUserSession {

    void start(String loginId);

    Optional<String> currentLoginId();

    void clear();
}