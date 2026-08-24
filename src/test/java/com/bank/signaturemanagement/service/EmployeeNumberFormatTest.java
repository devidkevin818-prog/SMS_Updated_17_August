package com.bank.signaturemanagement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeeNumberFormatTest {
    @Test
    void addsPrefixToSixDigitInput() {
        assertEquals("UB_PLC123456", EmployeeNumberFormat.normalize("123456"));
    }

    @Test
    void extractsEditableDigitsFromStoredNumber() {
        assertEquals("123456", EmployeeNumberFormat.editablePart("UB_PLC123456"));
    }

    @Test
    void rejectsNonNumericOrIncorrectLengthInput() {
        assertThrows(IllegalArgumentException.class, () -> EmployeeNumberFormat.normalize("12345"));
        assertThrows(IllegalArgumentException.class, () -> EmployeeNumberFormat.normalize("12345A"));
        assertThrows(IllegalArgumentException.class, () -> EmployeeNumberFormat.normalize("1234567"));
    }
}
