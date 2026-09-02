package com.bank.signaturemanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "import_batch_items")
public class ImportBatchItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ImportBatch batch;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @Column(name = "row_data", nullable = false, columnDefinition = "nvarchar(max)")
    private String rowData;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "error_detail", length = 1000)
    private String errorDetail;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    public Long getId() {
        return id;
    }

    public ImportBatch getBatch() {
        return batch;
    }

    public void setBatch(ImportBatch v) {
        batch = v;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int v) {
        rowNumber = v;
    }

    public String getRowData() {
        return rowData;
    }

    public void setRowData(String v) {
        rowData = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String v) {
        errorDetail = v;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee v) {
        employee = v;
    }
}
