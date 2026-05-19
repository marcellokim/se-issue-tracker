package com.github.marcellokim.issuetracker.domain;

import java.time.LocalDateTime;

public record ProjectMember(
                long projectId,
                String userId,
                LocalDateTime joinedAt) {
}
