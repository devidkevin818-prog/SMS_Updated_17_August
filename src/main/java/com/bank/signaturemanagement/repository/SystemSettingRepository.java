package com.bank.signaturemanagement.repository;

import com.bank.signaturemanagement.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKeyAndActiveTrue(String settingKey);
    Optional<SystemSetting> findBySettingKey(String settingKey);
}
