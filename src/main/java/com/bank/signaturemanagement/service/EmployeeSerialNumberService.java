package com.bank.signaturemanagement.service;
import com.bank.signaturemanagement.repository.EmployeeSerialNumberRepository;
import org.springframework.stereotype.Service;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.repository.EmployeeSerialNumberRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;



@Service
public class EmployeeSerialNumberService {

    private final EmployeeSerialNumberRepository serialNumberRepository;

    public EmployeeSerialNumberService(
            EmployeeSerialNumberRepository serialNumberRepository) {

        this.serialNumberRepository = serialNumberRepository;
    }

    @Transactional
    public EmployeeSerialNumber addSerialNumber(
            Long employeeId,
            Integer serialNumber) {

        validateEmployeeId(employeeId);
        validateSerialNumber(serialNumber);

        if (serialNumberRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalStateException(
                    "A serial-number record already exists for employee ID: "
                            + employeeId
                            + ". Use the update operation instead."
            );
        }

        if (serialNumberRepository.existsByNewSerialNumber(serialNumber)) {
            throw new IllegalStateException(
                    "Serial number " + serialNumber
                            + " is already assigned to another employee."
            );
        }

        EmployeeSerialNumber record = new EmployeeSerialNumber();

        record.setEmployeeId(employeeId);
        record.setNewSerialNumber(serialNumber);
        record.setOldSerialNumber(null);

        return serialNumberRepository.save(record);
    }

    @Transactional
    public EmployeeSerialNumber updateSerialNumber(
            Long employeeId,
            Integer newSerialNumber) {

        validateEmployeeId(employeeId);
        validateSerialNumber(newSerialNumber);

        EmployeeSerialNumber record = serialNumberRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No serial-number record found for employee ID: "
                                + employeeId
                ));

        Integer currentSerialNumber = record.getNewSerialNumber();

        if (newSerialNumber.equals(currentSerialNumber)) {
            throw new IllegalArgumentException(
                    "The new serial number is the same as the current serial number."
            );
        }

        if (serialNumberRepository.existsByNewSerialNumber(newSerialNumber)) {
            throw new IllegalStateException(
                    "Serial number " + newSerialNumber
                            + " is already assigned to another employee."
            );
        }

        record.setOldSerialNumber(currentSerialNumber);
        record.setNewSerialNumber(newSerialNumber);

        return serialNumberRepository.save(record);
    }

    @Transactional
    public EmployeeSerialNumber addOrUpdateSerialNumber(
            Long employeeId,
            Integer serialNumber) {

        validateEmployeeId(employeeId);
        validateSerialNumber(serialNumber);

        return serialNumberRepository
                .findByEmployeeId(employeeId)
                .map(existingRecord -> {
                    Integer currentSerialNumber =
                            existingRecord.getNewSerialNumber();

                    if (serialNumber.equals(currentSerialNumber)) {
                        return existingRecord;
                    }

                    if (serialNumberRepository
                            .existsByNewSerialNumber(serialNumber)) {

                        throw new IllegalStateException(
                                "Serial number " + serialNumber
                                        + " is already assigned to another employee."
                        );
                    }

                    existingRecord.setOldSerialNumber(currentSerialNumber);
                    existingRecord.setNewSerialNumber(serialNumber);

                    return serialNumberRepository.save(existingRecord);
                })
                .orElseGet(() -> {
                    if (serialNumberRepository
                            .existsByNewSerialNumber(serialNumber)) {

                        throw new IllegalStateException(
                                "Serial number " + serialNumber
                                        + " is already assigned to another employee."
                        );
                    }

                    EmployeeSerialNumber newRecord =
                            new EmployeeSerialNumber();

                    newRecord.setEmployeeId(employeeId);
                    newRecord.setNewSerialNumber(serialNumber);
                    newRecord.setOldSerialNumber(null);

                    return serialNumberRepository.save(newRecord);
                });
    }

    @Transactional(readOnly = true)
    public EmployeeSerialNumber getByEmployeeId(Long employeeId) {
        validateEmployeeId(employeeId);

        return serialNumberRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No serial-number record found for employee ID: "
                                + employeeId
                ));
    }

    @Transactional(readOnly = true)
    public List<EmployeeSerialNumber> getAll() {
        return serialNumberRepository.findAll();
    }

    private void validateEmployeeId(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException(
                    "A valid employee ID is required."
            );
        }
    }

    private void validateSerialNumber(Integer serialNumber) {
        if (serialNumber == null) {
            throw new IllegalArgumentException(
                    "Serial number is required."
            );
        }

        if (serialNumber <= 0) {
            throw new IllegalArgumentException(
                    "Serial number must be greater than zero."
            );
        }
    }
}