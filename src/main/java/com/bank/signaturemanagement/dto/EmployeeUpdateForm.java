package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class EmployeeUpdateForm {

    @NotBlank(message = "Employee code is required")
    @Pattern(
            regexp = "\\d{6}",
            message = "Employee code must contain exactly 6 digits"
    )
    private String employeeCode;

    @NotBlank(message = "Employee name is required")
    @Size(
            max = 150,
            message = "Employee name must not exceed 150 characters"
    )
    private String employeeName;

    @NotNull(message = "Designation is required")
    private Long designationId;

    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Branch is required")
    private Long branchId;

    private Long statusId;

    @NotBlank(message = "Classification is required")
    @Pattern(
            regexp = "LOCAL|FOREIGN|BOTH",
            message = "Select a valid classification"
    )
    private String classification = "BOTH";

    @NotNull(message = "Joining date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate signatureValidFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate signatureValidUntil;

    @NotBlank(message = "Update remark is required")
    @Size(
            max = 1000,
            message = "Update remark must not exceed 1000 characters"
    )
    private String remark;

    private MultipartFile photo;

    private MultipartFile signature;

    private MultipartFile foreignSignature;

    public EmployeeUpdateForm() {
    }

    /*
     * Validates that the signature expiry date is not earlier than
     * the signature effective date.
     *
     * If either date is empty, this validation succeeds because
     * the two signature-validity dates are currently optional.
     */
    @AssertTrue(
            message = "Signature valid until date cannot be earlier than signature valid from date"
    )
    public boolean isSignatureDateRangeValid() {
        if (signatureValidFrom == null || signatureValidUntil == null) {
            return true;
        }

        return !signatureValidUntil.isBefore(signatureValidFrom);
    }

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

    public Long getDesignationId() {
        return designationId;
    }

    public void setDesignationId(Long designationId) {
        this.designationId = designationId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
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

    public void setSignatureValidFrom(LocalDate signatureValidFrom) {
        this.signatureValidFrom = signatureValidFrom;
    }

    public LocalDate getSignatureValidUntil() {
        return signatureValidUntil;
    }

    public void setSignatureValidUntil(LocalDate signatureValidUntil) {
        this.signatureValidUntil = signatureValidUntil;
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

    public void setForeignSignature(MultipartFile foreignSignature) {
        this.foreignSignature = foreignSignature;
    }
}