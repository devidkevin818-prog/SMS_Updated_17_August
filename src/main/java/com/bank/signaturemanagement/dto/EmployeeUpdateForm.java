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
    @NotBlank private String designation;
    @NotBlank private String department;
    @NotBlank private String branch;
    @NotBlank private String remark;
    private MultipartFile photo;
    private MultipartFile signature;
    private MultipartFile foreignSignature;
    private Integer statusId;

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
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
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

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }
}
