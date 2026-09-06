package com.bank.signaturemanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only view model used by the role dashboards. */
public record DashboardDto(
        String username,
        String role,
        long totalEmployees,
        long activeSignatures,
        long pendingActions,
        long newEmployeesThisMonth,
        int signatureCoverage,
        List<String> monthLabels,
        List<Long> employeeCreated,
        List<Long> employeeUpdated,
        List<String> signatureLabels,
        List<Long> signatureValues,
        List<String> approvalLabels,
        List<Long> approvalValues,
        List<String> departmentLabels,
        List<Long> departmentValues,
        List<Activity> recentActivity) {

    public record Activity(LocalDateTime time, String actor, String action, String target, String result) {}
}
