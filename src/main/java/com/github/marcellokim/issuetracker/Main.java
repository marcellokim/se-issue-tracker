package com.github.marcellokim.issuetracker;

import com.github.marcellokim.issuetracker.cli.CommandLineEntryPoint;
import com.github.marcellokim.issuetracker.cli.ConsoleCliOutput;
import com.github.marcellokim.issuetracker.config.ApplicationBootstrap;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        new CommandLineEntryPoint(new ApplicationBootstrap(), new ConsoleCliOutput()).run(args);
    }
}
