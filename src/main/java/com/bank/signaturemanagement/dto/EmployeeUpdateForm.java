package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class EmployeeUpdateForm {
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Employee number must contain exactly 6 digits")
    private String employeeCode;
    @NotBlank private String employeeName;
    @NotNull private Long designationId;
    @NotNull private Long departmentId;
    @NotNull private Long branchId;
    @NotBlank private String remark;
    private MultipartFile photo;
    private MultipartFile signature;
    private MultipartFile foreignSignature;
    private Long statusId;
    @NotBlank @Pattern(regexp = "LOCAL|FOREIGN|BOTH", message = "Select a valid classification")
    private String classification = "BOTH";
    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    public MultipartFile getForeignSignature() {
        return foreignSignature;
    }

    public void setForeignSignature(MultipartFile foreignSignature) {
        this.foreignSignature = foreignSignature;
    }


    private LocalDate signatureValidFrom;

    private LocalDate signatureValidUntil;

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public Long getDesignationId() { return designationId; }
    public void setDesignationId(Long designationId) { this.designationId = designationId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public MultipartFile getPhoto() { return photo; }
    public void setPhoto(MultipartFile photo) { this.photo = photo; }
    public MultipartFile getSignature() { return signature; }
    public void setSignature(MultipartFile signature) { this.signature = signature; }

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

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
}
