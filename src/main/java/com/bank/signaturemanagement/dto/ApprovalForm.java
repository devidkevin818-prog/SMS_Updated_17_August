package com.bank.signaturemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class ApprovalForm {
    @NotBlank(message = "Remark is required")
    private String remark;

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
