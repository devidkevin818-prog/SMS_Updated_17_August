package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.SystemSetting;
import com.bank.signaturemanagement.repository.SystemSettingRepository;
import com.bank.signaturemanagement.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
public class SystemSettingService {
    private final SystemSettingRepository settings;
    private final UserRepository users;
    private final AccessControlService access;
    private final AuditService audit;

    public SystemSettingService(SystemSettingRepository settings, UserRepository users,
                                AccessControlService access, AuditService audit) {
        this.settings = settings; this.users = users; this.access = access; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public String employeeIdRegex() {
        return settings.findBySettingKeyAndActiveTrue(EmployeeNumberPolicyService.SETTING_KEY)
                .map(SystemSetting::getSettingValue).orElse(EmployeeNumberPolicyService.DEFAULT_REGEX);
    }

    @Transactional
    public void updateEmployeeIdRegex(String regex, String actor) {
        access.require(actor, "CONFIG_MANAGE");
        String value = regex == null ? "" : regex.trim();
        if (value.isEmpty() || value.length() > 1000) throw new IllegalArgumentException("Enter a valid employee-ID regular expression");
        try { Pattern.compile(value); }
        catch (PatternSyntaxException exception) { throw new IllegalArgumentException("Invalid regular expression: " + exception.getDescription()); }
        SystemSetting setting = settings.findBySettingKey(EmployeeNumberPolicyService.SETTING_KEY).orElseGet(() -> {
            SystemSetting created = new SystemSetting();
            created.setSettingKey(EmployeeNumberPolicyService.SETTING_KEY);
            created.setDescription("Regular expression applied to all newly entered employee IDs");
            return created;
        });
        String old = setting.getSettingValue();
        setting.setSettingValue(value); setting.setActive(true); setting.setUpdatedBy(users.findByUsername(actor).orElseThrow());
        setting.setUpdatedAt(LocalDateTime.now()); settings.save(setting);
        audit.record(actor,"CONFIG_UPDATE","SYSTEM_SETTING",EmployeeNumberPolicyService.SETTING_KEY,null,"SUCCESS",old,value,null);
    }
}
