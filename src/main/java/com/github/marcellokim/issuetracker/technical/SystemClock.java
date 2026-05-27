package com.github.marcellokim.issuetracker.technical;

import com.github.marcellokim.issuetracker.service.Clock;
import java.time.LocalDateTime;

public final class SystemClock implements Clock {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}