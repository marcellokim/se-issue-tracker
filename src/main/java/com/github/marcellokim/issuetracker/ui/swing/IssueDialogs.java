package com.github.marcellokim.issuetracker.ui.swing;

import java.util.Optional;

interface IssueDialogs {

    Optional<IssueRegisterRequest> requestRegister(IssueListPanel parent);
}
