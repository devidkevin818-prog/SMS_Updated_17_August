package com.bank.signaturemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmployeeSignatureApplication {

    public static void main(String[] args) {
        SqlServerDatabaseProvisioner.createConfiguredDatabaseIfMissing(args);
        SpringApplication.run(EmployeeSignatureApplication.class, args);
    }
}
