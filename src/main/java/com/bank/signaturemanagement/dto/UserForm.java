package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserForm {
    @NotBlank @Size(max = 50, message = "Username must not exceed 50 characters") private String username;
    @NotBlank @Size(min = 8, message = "Password must contain at least 8 characters") private String password;
    @NotBlank @Size(max = 100, message = "Full name must not exceed 100 characters") private String fullName;
    @NotBlank @Email @Size(max = 100, message = "Email must not exceed 100 characters") private String email;
    @NotBlank @Size(max = 100, message = "Branch must not exceed 100 characters") private String branchId;
    @NotBlank @Size(max = 30) private String roleName;


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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
