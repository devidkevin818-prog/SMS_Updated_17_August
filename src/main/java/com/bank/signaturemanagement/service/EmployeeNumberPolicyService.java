package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class EmployeeNumberPolicyService {
    public static final String SETTING_KEY = "EMPLOYEE_ID_REGEX";
    public static final String DEFAULT_REGEX = "^UB_PLC[0-9]{6}$";
    private final SystemSettingRepository settings;

    public EmployeeNumberPolicyService(SystemSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public String normalize(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Employee ID is required");
        String candidate = input.trim().toUpperCase(Locale.ROOT);
        if (candidate.matches("[0-9]{6}")) candidate = EmployeeNumberFormat.PREFIX + candidate;
        String regex = regex();
        if (!Pattern.compile(regex).matcher(candidate).matches()) {
            throw new IllegalArgumentException("Employee ID does not match the configured format: " + regex);
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public String regex() {
        return settings.findBySettingKeyAndActiveTrue(SETTING_KEY)
                .map(setting -> setting.getSettingValue()).orElse(DEFAULT_REGEX);
    }
}
