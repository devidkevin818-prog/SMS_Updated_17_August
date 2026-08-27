package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeSignatureSerialNumber;
import com.bank.signaturemanagement.repository.EmployeeSignatureSerialNumberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmployeeSignatureSerialNumberService {

    private final EmployeeSignatureSerialNumberRepository repository;

    public EmployeeSignatureSerialNumberService(
            EmployeeSignatureSerialNumberRepository repository) {

        this.repository = repository;
    }

    public EmployeeSignatureSerialNumber getByEmployeeId(Long employeeId) {

        return repository.findByEmployeeId(employeeId)
                .orElse(null);
    }

    public Integer get2025SerialNumber(Long employeeId) {

        return repository.findByEmployeeId(employeeId)
                .map(EmployeeSignatureSerialNumber::getSerialNumber2025)
                .orElse(null);
    }

    public Integer get2026SerialNumber(Long employeeId) {

        return repository.findByEmployeeId(employeeId)
                .map(EmployeeSignatureSerialNumber::getSerialNumber2026)
                .orElse(null);
    }

    public Page<EmployeeSignatureSerialNumber> getEmployeesWithSerialNumbers(
            String query,
            int page) {

        // Prevent invalid page numbers
        if (page < 0) {
            page = 0;
        }

        // 20 records per page
        Pageable pageable = PageRequest.of(page, 20);

        // Clean the search value
        String search = query == null
                ? ""
                : query.trim();

        // Database performs the employee + serial-number join
        // and the search.
        return repository.findWithEmployee(
                search,
                pageable
        );
    }
    public void update(
            Long id,
            EmployeeSignatureSerialNumber input) {

        EmployeeSignatureSerialNumber existing =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Signature serial number not found: " + id));

        existing.setSerialNumber2025(input.getSerialNumber2025());
        existing.setSerialNumber2026(input.getSerialNumber2026());
//        existing.setStatus(input.getStatus());

        repository.save(existing);
    }

    public EmployeeSignatureSerialNumber saveEmptySerialNumber(Employee employee) {

        EmployeeSignatureSerialNumber serial =
                new EmployeeSignatureSerialNumber();

        serial.setEmployee(employee);
        serial.setSerialNumber2025(null);
        serial.setSerialNumber2026(null);

        return repository.save(serial);
    }



    }



