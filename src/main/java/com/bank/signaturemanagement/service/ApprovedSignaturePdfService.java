package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ApprovedSignaturePdfService {

    private final EmployeeRepository employeeRepository;

    // =========================================================
    // File Storage Configuration
    // =========================================================
    //
    // application.properties:
    //
    // app.upload.root=E:/images
    // app.profile-photo.root=E:/images/employee-photo
    // app.signature.root=E:/images/employee-signature
    //
    // Database:
    //
    // profile/1/2a84ed3b-4145-424a-9fae-2e3cdc9fbc0a.jpg
    // signature/1/bbb7c067-c104-4cc4-89db-b0a6f0a09aaf.png
    //
    // Actual files:
    //
    // E:/images/employee-photo/1/2a84ed3b-4145-424a-9fae-2e3cdc9fbc0a.jpg
    // E:/images/employee-signature/1/bbb7c067-c104-4cc4-89db-b0a6f0a09aaf.png
    //
    // =========================================================

    @Value("${app.profile-photo.root}")
    private String profilePhotoRoot;

    @Value("${app.signature.root}")
    private String signatureRoot;

    public ApprovedSignaturePdfService(
            EmployeeRepository employeeRepository
    ) {
        this.employeeRepository = employeeRepository;
    }

    // =========================================================
    // Generate PDF
    // =========================================================

    public void generateApprovedPdf(
            OutputStream outputStream
    ) throws Exception {

        // Get employees
        List<Employee> employees =
                employeeRepository.findAll();

        // =====================================================
        // PDF Document
        // A3 Landscape gives more horizontal space
        // =====================================================

        Document document = new Document(
                PageSize.A3.rotate(),
                30,
                30,
                30,
                30
        );

        PdfWriter.getInstance(
                document,
                outputStream
        );

        document.open();

        // =====================================================
        // Title
        // =====================================================

        Font titleFont = new Font(
                Font.HELVETICA,
                30,
                Font.BOLD
        );

        Paragraph title = new Paragraph(
                "Approved Employee Photos and Signatures",
                titleFont
        );

        title.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(title);

        Paragraph titleSpace = new Paragraph(" ");
        titleSpace.setSpacingAfter(20f);
        document.add(titleSpace);

        // =====================================================
        // Table
        // =====================================================

        PdfPTable table =
                new PdfPTable(9);

        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                1.0f,
                1.5f,
                2.5f,
                2.5f,
                2.5f,
                2.5f,
                2.5f,
                2.5f,
                3.0f
        });

        // =====================================================
        // Table Headers
        // =====================================================

        addHeader(table, "SL");
        addHeader(table, "Code");
        addHeader(table, "Name");
        addHeader(table, "Designation");
        addHeader(table, "Department");
        addHeader(table, "Branch");
        addHeader(table, "Signature Validity");
        addHeader(table, "Photo");
        addHeader(table, "Signature");

        // =====================================================
        // Employee Rows
        // =====================================================

        int serial = 1;

        for (Employee employee : employees) {

            // =================================================
            // SL
            // =================================================

            addCenteredCell(
                    table,
                    String.valueOf(serial)
            );

            serial++;

            // =================================================
            // Employee Code
            // =================================================

            addCell(
                    table,
                    employee.getEmployeeNumber()
            );

            // =================================================
            // Employee Name
            // =================================================

            addCell(
                    table,
                    employee.getFullName()
            );

            // =================================================
            // Designation
            // =================================================

            addCell(
                    table,
                    employee.getDesignation()
            );

            // =================================================
            // Department
            // =================================================

            addCell(
                    table,
                    employee.getDepartment()
            );

            // =================================================
            // Branch
            // =================================================

            addCell(
                    table,
                    employee.getBranchCode()
            );

            // =================================================
            // Signature Validity
            // =================================================

            String validity =
                    String.valueOf(
                            employee.getSignatureValidFrom()
                    )
                            + "\n"
                            + "to"
                            + "\n"
                            + String.valueOf(
                            employee.getSignatureValidUntil()
                    );

            addCenteredCell(
                    table,
                    validity
            );

            // =================================================
            // PHOTO
            // =================================================

            PdfPCell photoCell =
                    new PdfPCell();

            photoCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            photoCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            photoCell.setPadding(8);

            String photoDbPath =
                    employee.getPhotoPath();

            if (photoDbPath != null &&
                    !photoDbPath.isBlank()) {

                Path photoPath =
                        resolvePhotoPath(photoDbPath);

                if (Files.exists(photoPath)
                        && Files.isRegularFile(photoPath)) {

                    Image photo =
                            Image.getInstance(
                                    photoPath
                                            .toAbsolutePath()
                                            .toString()
                            );

                    photo.scaleToFit(
                            100,
                            100
                    );

                    photoCell.addElement(
                            photo
                    );

                } else {

                    photoCell.addElement(
                            new Paragraph(
                                    "Photo not found"
                            )
                    );
                }

            } else {

                photoCell.addElement(
                        new Paragraph(
                                "Photo not found"
                        )
                );
            }

            table.addCell(photoCell);

            // =================================================
            // SIGNATURE
            // =================================================

            PdfPCell signatureCell =
                    new PdfPCell();

            signatureCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            signatureCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            signatureCell.setPadding(8);

            String signatureDbPath =
                    employee.getSignaturePath();

            if (signatureDbPath != null &&
                    !signatureDbPath.isBlank()) {

                Path signaturePath =
                        resolveSignaturePath(
                                signatureDbPath
                        );

                if (Files.exists(signaturePath)
                        && Files.isRegularFile(signaturePath)) {

                    Image signature =
                            Image.getInstance(
                                    signaturePath
                                            .toAbsolutePath()
                                            .toString()
                            );

                    signature.scaleToFit(
                            160,
                            80
                    );

                    signatureCell.addElement(
                            signature
                    );

                } else {

                    signatureCell.addElement(
                            new Paragraph(
                                    "Signature not found"
                            )
                    );
                }

            } else {

                signatureCell.addElement(
                        new Paragraph(
                                "Signature not found"
                        )
                );
            }

            table.addCell(signatureCell);
        }

        // =====================================================
        // Add Table
        // =====================================================

        document.add(table);

        // =====================================================
        // Close PDF
        // =====================================================

        document.close();
    }

    // =========================================================
    // Resolve Photo Path
    // =========================================================
    //
    // DB:
    //
    // profile/1/photo.jpg
    //
    // Becomes:
    //
    // E:/images/employee-photo/1/photo.jpg
    //
    // =========================================================

    private Path resolvePhotoPath(
            String dbPath
    ) {

        String relativePath = dbPath;

        if (relativePath.startsWith("profile/")) {

            relativePath =
                    relativePath.substring(
                            "profile/".length()
                    );
        }

        Path root = Paths
                .get(profilePhotoRoot)
                .toAbsolutePath()
                .normalize();

        return root
                .resolve(relativePath)
                .normalize();
    }

    // =========================================================
    // Resolve Signature Path
    // =========================================================
    //
    // DB:
    //
    // signature/1/signature.png
    //
    // Becomes:
    //
    // E:/images/employee-signature/1/signature.png
    //
    // =========================================================

    private Path resolveSignaturePath(
            String dbPath
    ) {

        String relativePath = dbPath;

        if (relativePath.startsWith("signature/")) {

            relativePath =
                    relativePath.substring(
                            "signature/".length()
                    );
        }

        Path root = Paths
                .get(signatureRoot)
                .toAbsolutePath()
                .normalize();

        return root
                .resolve(relativePath)
                .normalize();
    }

    // =========================================================
    // Header Cell
    // =========================================================

    private void addHeader(
            PdfPTable table,
            String text
    ) {

        Font headerFont =
                new Font(
                        Font.HELVETICA,
                        20,
                        Font.BOLD
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                headerFont
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(8);

        table.addCell(cell);
    }

    // =========================================================
    // Normal Cell
    // =========================================================

    private void addCell(
            PdfPTable table,
            String text
    ) {

        Font cellFont =
                new Font(
                        Font.HELVETICA,
                        16,
                        Font.NORMAL
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text == null ? "" : text,
                                cellFont
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(8);

        table.addCell(cell);
    }

    // =========================================================
    // Centered Cell
    // =========================================================

    private void addCenteredCell(
            PdfPTable table,
            String text
    ) {

        Font cellFont =
                new Font(
                        Font.HELVETICA,
                        16,
                        Font.NORMAL
                );

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text == null
                                        ? ""
                                        : text,
                                cellFont
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(8);

        table.addCell(cell);
    }
}
