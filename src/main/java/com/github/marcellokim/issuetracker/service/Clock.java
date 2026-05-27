package com.github.marcellokim.issuetracker.service;

import java.time.LocalDateTime;

public interface Clock {
    LocalDateTime now();
}