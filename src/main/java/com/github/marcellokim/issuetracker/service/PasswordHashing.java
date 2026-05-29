package com.github.marcellokim.issuetracker.service;

public interface PasswordHashing {

    String hash(String password);

    boolean matches(String password, String storedCredential);

    boolean isHashed(String storedCredential);

    String saltOf(String storedCredential);

    String hashOf(String storedCredential);
}