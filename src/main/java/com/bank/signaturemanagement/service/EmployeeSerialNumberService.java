package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.bank.signaturemanagement.repository.EmployeeSerialNumberRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeSerialNumberService {

    private final EmployeeSerialNumberRepository serialNumberRepository;
    private final EmployeeRepository employeeRepository;

    public EmployeeSerialNumberService(
            EmployeeSerialNumberRepository serialNumberRepository,
            EmployeeRepository employeeRepository
    ) {
        this.serialNumberRepository = serialNumberRepository;
        this.employeeRepository = employeeRepository;
    }

    /*
     * Saves a new local serial-number history record.
     */
    @Transactional
    public EmployeeSerialNumber addLocalSerialNumber(
            Long employeeId,
            Integer localSerialNumber
    ) {
        validateEmployeeId(employeeId);
        validateSerialNumber(localSerialNumber, "Local serial number");

        Employee employee = getEmployee(employeeId);

        EmployeeSerialNumber latestRecord =
                serialNumberRepository
                        .findTopByEmployee_IdOrderByCreatedAtDesc(employeeId)
                        .orElse(null);

        Integer currentLocalSerial = latestRecord != null
                ? latestRecord.getNewLocalSerial()
                : null;

        if (localSerialNumber.equals(currentLocalSerial)) {
            throw new IllegalArgumentException(
                    "The new local serial number is the same as "
                            + "the employee's current local serial number."
            );
        }

        if (serialNumberRepository
                .existsByNewLocalSerial(localSerialNumber)) {

            throw new IllegalStateException(
                    "Local serial number " + localSerialNumber
                            + " is already assigned."
            );
        }

        EmployeeSerialNumber history = new EmployeeSerialNumber();

        history.setEmployee(employee);
        history.setOldLocalSerial(currentLocalSerial);
        history.setNewLocalSerial(localSerialNumber);

        copyForeignSerials(latestRecord, history);

        return serialNumberRepository.save(history);
    }

    /*
     * Saves a new foreign serial-number history record.
     */
    @Transactional
    public EmployeeSerialNumber addForeignSerialNumber(
            Long employeeId,
            Integer foreignSerialNumber
    ) {
        validateEmployeeId(employeeId);
        validateSerialNumber(foreignSerialNumber, "Foreign serial number");

        Employee employee = getEmployee(employeeId);

        EmployeeSerialNumber latestRecord =
                serialNumberRepository
                        .findTopByEmployee_IdOrderByCreatedAtDesc(employeeId)
                        .orElse(null);

        Integer currentForeignSerial = latestRecord != null
                ? latestRecord.getNewForeignSerial()
                : null;

        if (foreignSerialNumber.equals(currentForeignSerial)) {
            throw new IllegalArgumentException(
                    "The new foreign serial number is the same as "
                            + "the employee's current foreign serial number."
            );
        }

        if (serialNumberRepository
                .existsByNewForeignSerial(foreignSerialNumber)) {

            throw new IllegalStateException(
                    "Foreign serial number " + foreignSerialNumber
                            + " is already assigned."
            );
        }

        EmployeeSerialNumber history = new EmployeeSerialNumber();

        history.setEmployee(employee);
        history.setOldForeignSerial(currentForeignSerial);
        history.setNewForeignSerial(foreignSerialNumber);

        copyLocalSerials(latestRecord, history);

        return serialNumberRepository.save(history);
    }

    /*
     * Updates local and foreign serial numbers together.
     *
     * A null value means that serial type should not be changed.
     */
    @Transactional
    public EmployeeSerialNumber updateSerialNumbers(
            Long employeeId,
            Integer newLocalSerial,
            Integer newForeignSerial
    ) {
        validateEmployeeId(employeeId);

        if (newLocalSerial == null && newForeignSerial == null) {
            throw new IllegalArgumentException(
                    "At least one serial number must be provided."
            );
        }

        if (newLocalSerial != null) {
            validateSerialNumber(
                    newLocalSerial,
                    "Local serial number"
            );
        }

        if (newForeignSerial != null) {
            validateSerialNumber(
                    newForeignSerial,
                    "Foreign serial number"
            );
        }

        Employee employee = getEmployee(employeeId);

        EmployeeSerialNumber latestRecord =
                serialNumberRepository
                        .findTopByEmployee_IdOrderByCreatedAtDesc(employeeId)
                        .orElse(null);

        Integer currentLocalSerial = latestRecord != null
                ? latestRecord.getNewLocalSerial()
                : null;

        Integer currentForeignSerial = latestRecord != null
                ? latestRecord.getNewForeignSerial()
                : null;

        Integer resultingLocalSerial = newLocalSerial != null
                ? newLocalSerial
                : currentLocalSerial;

        Integer resultingForeignSerial = newForeignSerial != null
                ? newForeignSerial
                : currentForeignSerial;

        boolean localChanged = newLocalSerial != null
                && !newLocalSerial.equals(currentLocalSerial);

        boolean foreignChanged = newForeignSerial != null
                && !newForeignSerial.equals(currentForeignSerial);

        if (!localChanged && !foreignChanged) {
            throw new IllegalArgumentException(
                    "The provided serial numbers are the same as "
                            + "the employee's current serial numbers."
            );
        }

        if (localChanged
                && serialNumberRepository
                .existsByNewLocalSerial(newLocalSerial)) {

            throw new IllegalStateException(
                    "Local serial number " + newLocalSerial
                            + " is already assigned."
            );
        }

        if (foreignChanged
                && serialNumberRepository
                .existsByNewForeignSerial(newForeignSerial)) {

            throw new IllegalStateException(
                    "Foreign serial number " + newForeignSerial
                            + " is already assigned."
            );
        }

        EmployeeSerialNumber history = new EmployeeSerialNumber();

        history.setEmployee(employee);

        history.setOldLocalSerial(
                localChanged ? currentLocalSerial : null
        );
        history.setNewLocalSerial(resultingLocalSerial);

        history.setOldForeignSerial(
                foreignChanged ? currentForeignSerial : null
        );
        history.setNewForeignSerial(resultingForeignSerial);

        return serialNumberRepository.save(history);
    }

    /*
     * Returns the latest serial-number record for an employee.
     */
    @Transactional(readOnly = true)
    public EmployeeSerialNumber getLatestByEmployeeId(Long employeeId) {
        validateEmployeeId(employeeId);

        return serialNumberRepository
                .findTopByEmployee_IdOrderByCreatedAtDesc(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No serial-number history found for employee ID: "
                                + employeeId
                ));
    }

    /*
     * Returns the complete history for an employee.
     */
    @Transactional(readOnly = true)
    public List<EmployeeSerialNumber> getHistoryByEmployeeId(
            Long employeeId
    ) {
        validateEmployeeId(employeeId);

        return serialNumberRepository
                .findByEmployee_IdOrderByCreatedAtDesc(employeeId);
    }

    /*
     * Returns all serial-number history records.
     */
    @Transactional(readOnly = true)
    public List<EmployeeSerialNumber> getAll() {
        return serialNumberRepository
                .findAllByOrderByCreatedAtDesc();
    }

    private Employee getEmployee(Long employeeId) {
        return employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found with ID: " + employeeId
                ));
    }

    private void copyLocalSerials(
            EmployeeSerialNumber source,
            EmployeeSerialNumber target
    ) {
        if (source != null) {
            target.setNewLocalSerial(source.getNewLocalSerial());
        }
    }

    private void copyForeignSerials(
            EmployeeSerialNumber source,
            EmployeeSerialNumber target
    ) {
        if (source != null) {
            target.setNewForeignSerial(source.getNewForeignSerial());
        }
    }

    private void validateEmployeeId(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException(
                    "A valid employee ID is required."
            );
        }
    }

    private void validateSerialNumber(
            Integer serialNumber,
            String fieldName
    ) {
        if (serialNumber == null) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        if (serialNumber <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }
    }
    @Transactional(readOnly = true)
    public Optional<EmployeeSerialNumber> findLatestByEmployeeId(
            Long employeeId
    ) {
        validateEmployeeId(employeeId);

        return serialNumberRepository
                .findTopByEmployee_IdOrderByCreatedAtDesc(employeeId);
    }

}