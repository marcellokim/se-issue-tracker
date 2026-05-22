package com.github.marcellokim.issuetracker.cli;

@FunctionalInterface
public interface CliOutput {

    void println(String message);
}
