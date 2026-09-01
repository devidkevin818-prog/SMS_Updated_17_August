package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserForm {
    @NotBlank @Size(max = 50, message = "Username must not exceed 50 characters") private String username;
    @NotBlank @Size(min = 8, message = "Password must contain at least 8 characters") private String password;
    @NotBlank @Size(max = 100, message = "Full name must not exceed 100 characters") private String fullName;
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Employee ID must contain exactly 6 digits")
    private String employeeNumber;
    @NotBlank @Email @Size(max = 100, message = "Email must not exceed 100 characters") private String email;
    @NotBlank @Size(max = 100, message = "Branch must not exceed 100 characters") private String branchId;
    @NotBlank @Size(max = 30) private String roleName;
    @NotBlank @Pattern(regexp = "LOCAL|FOREIGN|BOTH", message = "Select a valid signature scope")
    private String signatureScope = "BOTH";


    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getSignatureScope() { return signatureScope; }
    public void setSignatureScope(String signatureScope) { this.signatureScope = signatureScope; }
}
