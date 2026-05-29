package com.github.marcellokim.issuetracker.repository;

import com.github.marcellokim.issuetracker.domain.DashboardProjectSnapshot;
import java.util.List;

public interface DashboardSummaryRepository {

    List<DashboardProjectSnapshot> findAllProjectSummaries();

    List<DashboardProjectSnapshot> findProjectSummariesByParticipant(String loginId);
}
