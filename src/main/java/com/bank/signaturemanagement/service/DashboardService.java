package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.dto.DashboardDto;
import com.bank.signaturemanagement.entity.RequestStatus;
import com.bank.signaturemanagement.repository.AuditLogRepository;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.EmployeeRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final EmployeeRepository employees;
    private final EmployeeRequestRepository requests;
    private final AuditLogRepository auditLogs;

    public DashboardService(
            EmployeeRepository employees,
            EmployeeRequestRepository requests,
            AuditLogRepository auditLogs
    ) {
        this.employees = employees;
        this.requests = requests;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboardData(
            String username,
            String role
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart =
                today.withDayOfMonth(1).atStartOfDay();

        long total = employees.count();
        long signed = employees.countWithSignature();

        long pendingLevel1Checker =
                requests.countByStatus(
                        RequestStatus.PENDING_DGM
                );

        long pendingLevel2Checker =
                requests.countByStatus(
                        RequestStatus.PENDING_GM
                );

        long pending = switch (role) {
            case "LEVEL_1_CHECKER" ->
                    pendingLevel1Checker;

            case "LEVEL_2_CHECKER" ->
                    pendingLevel2Checker;

            case "MAKER" ->
                    requests.countByRequestedByUsernameAndStatusIn(
                            username,
                            List.of(
                                    RequestStatus.PENDING_DGM,
                                    RequestStatus.PENDING_GM
                            )
                    );

            default ->
                    pendingLevel1Checker + pendingLevel2Checker;
        };

        List<String> months = new ArrayList<>();
        List<Long> created = new ArrayList<>();
        List<Long> updated = new ArrayList<>();

        DateTimeFormatter monthFormat =
                DateTimeFormatter.ofPattern("MMM");

        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month =
                    YearMonth.from(today).minusMonths(offset);

            LocalDateTime start =
                    month.atDay(1).atStartOfDay();

            LocalDateTime end =
                    month.plusMonths(1)
                            .atDay(1)
                            .atStartOfDay();

            months.add(
                    month.format(monthFormat)
            );

            created.add(
                    employees
                            .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                    start,
                                    end
                            )
            );

            updated.add(
                    employees
                            .countByUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                                    start,
                                    end
                            )
            );
        }

        long expired =
                employees.countExpiredSignatures(today);

        long valid =
                employees.countValidSignatures(today);

        long missing =
                employees.countMissingSignatures();

        List<Object[]> distribution =
                employees.countByDepartment();

        List<DashboardDto.Activity> activity =
                auditLogs.search(
                                "",
                                "",
                                PageRequest.of(0, 8)
                        )
                        .stream()
                        .map(log ->
                                new DashboardDto.Activity(
                                        log.getEventTime(),
                                        log.getUsername(),
                                        log.getActionType(),
                                        log.getTargetId(),
                                        log.getResult()
                                )
                        )
                        .toList();

        return new DashboardDto(
                username,
                role,
                total,
                signed,
                pending,
                employees
                        .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                monthStart,
                                now.plusSeconds(1)
                        ),
                total == 0
                        ? 0
                        : (int) Math.round(
                        signed * 100.0 / total
                ),
                months,
                created,
                updated,
                List.of(
                        "Valid",
                        "Expired",
                        "Missing"
                ),
                List.of(
                        valid,
                        expired,
                        missing
                ),
                List.of(
                        "Awaiting Level 1 Checker",
                        "Awaiting Level 2 Checker"
                ),
                List.of(
                        pendingLevel1Checker,
                        pendingLevel2Checker
                ),
                distribution.stream()
                        .map(row ->
                                String.valueOf(row[0])
                        )
                        .toList(),
                distribution.stream()
                        .map(row ->
                                (Long) row[1]
                        )
                        .toList(),
                activity
        );
    }
}