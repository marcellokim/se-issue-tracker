package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.IssueDetailResult;
import com.github.marcellokim.issuetracker.service.IssueWorkflowActions;
import java.util.List;

interface IssueDetailView {

    void showDetail(IssueDetailResult detail, List<IssueCommentActionState> commentActions);

    void showActions(IssueWorkflowActions actions);

    void showMessage(String message, boolean error);
}
