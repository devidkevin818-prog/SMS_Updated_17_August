package com.bank.signaturemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureTemporaryPasswordGeneratorTest {
    @Test
    void generatesDistinctPasswordsWithRequiredCharacterGroups() {
        SecureTemporaryPasswordGenerator generator = new SecureTemporaryPasswordGenerator();
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
        assertTrue(first.length() >= 8);
        assertTrue(first.chars().anyMatch(Character::isUpperCase));
        assertTrue(first.chars().anyMatch(Character::isLowerCase));
        assertTrue(first.chars().anyMatch(Character::isDigit));
        assertTrue(first.chars().anyMatch(value -> "@#$%&*!?".indexOf(value) >= 0));
    }
}
