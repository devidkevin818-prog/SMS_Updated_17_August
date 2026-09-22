package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class EmployeeRequestForm {

    @NotBlank
    @Pattern(
            regexp = "\\d{6}",
            message = "Employee number must contain exactly 6 digits"
    )
    private String employeeCode;

    @NotBlank
    private String employeeName;

    @NotNull(message = "Designation is required")
    private Long designation;

    @NotNull(message = "Department is required")
    private Long department;

    @NotNull(message = "Branch is required")
    private Long branch;

    @NotBlank
    private String remark;

    private MultipartFile photo;

    private MultipartFile signature;

    private MultipartFile foreignSignature;

    private Long statusId;

    @NotBlank
    @Pattern(
            regexp = "LOCAL|FOREIGN|BOTH",
            message = "Select a valid classification"
    )
    private String classification = "BOTH";

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate signatureValidFrom;

    private LocalDate signatureValidUntil;

    /*
     * A value of 0 means that the employee does not have
     * a local signature serial number.
     */
    @NotNull(message = "Local signature serial number is required")
    @Min(
            value = 0,
            message = "Local signature serial number cannot be negative"
    )
    private Integer localSerialNumber = 0;

    /*
     * A value of 0 means that the employee does not have
     * a foreign signature serial number.
     */
    @NotNull(message = "Foreign signature serial number is required")
    @Min(
            value = 0,
            message = "Foreign signature serial number cannot be negative"
    )
    private Integer foreignSerialNumber = 0;

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Long getDesignation() {
        return designation;
    }

    public void setDesignation(Long designation) {
        this.designation = designation;
    }

    public Long getDepartment() {
        return department;
    }

    public void setDepartment(Long department) {
        this.department = department;
    }

    public Long getBranch() {
        return branch;
    }

    public void setBranch(Long branch) {
        this.branch = branch;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }

    public MultipartFile getSignature() {
        return signature;
    }

    public void setSignature(MultipartFile signature) {
        this.signature = signature;
    }

    public MultipartFile getForeignSignature() {
        return foreignSignature;
    }

    public void setForeignSignature(
            MultipartFile foreignSignature
    ) {
        this.foreignSignature = foreignSignature;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public LocalDate getSignatureValidFrom() {
        return signatureValidFrom;
    }

    public void setSignatureValidFrom(
            LocalDate signatureValidFrom
    ) {
        this.signatureValidFrom = signatureValidFrom;
    }

    public LocalDate getSignatureValidUntil() {
        return signatureValidUntil;
    }

    public void setSignatureValidUntil(
            LocalDate signatureValidUntil
    ) {
        this.signatureValidUntil = signatureValidUntil;
    }

    public Integer getLocalSerialNumber() {
        return localSerialNumber;
    }

    public void setLocalSerialNumber(
            Integer localSerialNumber
    ) {
        this.localSerialNumber =
                localSerialNumber != null
                        ? localSerialNumber
                        : 0;
    }

    public Integer getForeignSerialNumber() {
        return foreignSerialNumber;
    }

    public void setForeignSerialNumber(
            Integer foreignSerialNumber
    ) {
        this.foreignSerialNumber =
                foreignSerialNumber != null
                        ? foreignSerialNumber
                        : 0;
    }
}