package com.bank.signaturemanagement.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureTemporaryPasswordGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*!?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int LENGTH = 12;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] password = new char[LENGTH];
        password[0] = randomCharacter(UPPER);
        password[1] = randomCharacter(LOWER);
        password[2] = randomCharacter(DIGITS);
        password[3] = randomCharacter(SPECIAL);
        for (int index = 4; index < password.length; index++) password[index] = randomCharacter(ALL);
        for (int index = password.length - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            char value = password[index];
            password[index] = password[other];
            password[other] = value;
        }
        return new String(password);
    }

    private char randomCharacter(String characters) {
        return characters.charAt(random.nextInt(characters.length()));
    }
}
