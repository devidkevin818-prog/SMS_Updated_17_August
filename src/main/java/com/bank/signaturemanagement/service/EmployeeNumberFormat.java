package com.bank.signaturemanagement.service;

import java.util.regex.Pattern;

public final class EmployeeNumberFormat {
    public static final String PREFIX = "UB_PLC";
    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    private EmployeeNumberFormat() {
    }

    public static String normalize(String input) {
        String digits = editablePart(input);
        if (!SIX_DIGITS.matcher(digits).matches()) {
            throw new IllegalArgumentException("Employee ID must contain exactly 6 digits after UB_PLC");
        }
        return PREFIX + digits;
    }

    public static String editablePart(String employeeNumber) {
        if (employeeNumber == null) return "";
        String value = employeeNumber.trim().toUpperCase();
        return value.startsWith(PREFIX) ? value.substring(PREFIX.length()) : value;
    }
}
