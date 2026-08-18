package com.bank.signaturemanagement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EmployeeSignatureApplicationTests {
    @Test
    void applicationEntryPointCanBeLoaded() {
        assertDoesNotThrow(() -> Class.forName(EmployeeSignatureApplication.class.getName()));
    }
}
