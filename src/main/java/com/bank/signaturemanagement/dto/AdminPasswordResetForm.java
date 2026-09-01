package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.Pattern;

public class AdminPasswordResetForm {
    @Pattern(regexp = "generate|manual", message = "Select a valid reset method")
    private String resetMethod = "generate";
    private String generatedPassword;
    private String newPassword;
    private String confirmPassword;
    private boolean requirePasswordChange = true;

    public String getResetMethod() {
        return resetMethod;
    }

    public void setResetMethod(String resetMethod) {
        this.resetMethod = resetMethod;
    }

    public String getGeneratedPassword() {
        return generatedPassword;
    }

    public void setGeneratedPassword(String generatedPassword) {
        this.generatedPassword = generatedPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isRequirePasswordChange() {
        return requirePasswordChange;
    }

    public void setRequirePasswordChange(boolean requirePasswordChange) {
        this.requirePasswordChange = requirePasswordChange;
    }
}
