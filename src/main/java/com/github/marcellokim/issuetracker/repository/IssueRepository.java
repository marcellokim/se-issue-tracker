package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.Issue;
import java.util.Optional;

public interface IssueRepository {

    Optional<Issue> findById(String issueId);

    void save(Issue issue);
}
