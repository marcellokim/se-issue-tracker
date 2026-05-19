package com.github.marcellokim.issuetracker.service;

import java.time.LocalDateTime;

public final class Clock {

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
