package com.github.marcellokim.issuetracker.technical;

public final class ConsoleOutput {

    private ConsoleOutput() {
    }

    @SuppressWarnings("java:S106")
    public static void out(String message) {
        System.out.println(message);
    }

    @SuppressWarnings("java:S106")
    public static void err(String message) {
        System.err.println(message);
    }
}
