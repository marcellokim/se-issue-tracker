package com.github.marcellokim.issuetracker.cli;

import com.github.marcellokim.issuetracker.technical.ConsoleOutput;

public final class ConsoleCliOutput implements CliOutput {

    @Override
    public void println(String message) {
        ConsoleOutput.out(message);
    }
}
