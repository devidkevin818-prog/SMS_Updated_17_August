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
    // Upload Root
    // =========================================================
    //
    // Database path example:
    //
    // profile/6/photo.jpg
    // signature/6/signature.png
    //
    // Actual file:
    //
    // uploads/profile/6/photo.jpg
    // uploads/signature/6/signature.png
    //
    // =========================================================

    @Value("${app.upload.root}")
    private String uploadRoot;

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

        // Space below title
        Paragraph titleSpace = new Paragraph(" ");
        titleSpace.setSpacingAfter(20f);
        document.add(titleSpace);

        // =====================================================
        // Table
        //
        // 1. SL
        // 2. Code
        // 3. Name
        // 4. Designation
        // 5. Department
        // 6. Branch
        // 7. Signature Validity
        // 8. Photo
        // 9. Signature
        //
        // =====================================================

        PdfPTable table =
                new PdfPTable(9);

        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                1.0f,   // SL
                1.5f,   // Code
                2.5f,   // Name
                2.5f,   // Designation
                2.5f,   // Department
                2.5f,   // Branch
                2.5f,   // Signature Validity
                2.5f,   // Photo
                3.0f    // Signature
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
        // Upload Root
        // =====================================================

        Path root = Paths
                .get(uploadRoot)
                .toAbsolutePath()
                .normalize();

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
            //
            // Three lines:
            //
            // 2026-08-18
            // to
            // 2026-09-30
            //
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

                /*
                 * Example DB path:
                 *
                 * profile/6/f35d....jpg
                 *
                 * Actual:
                 *
                 * uploads/profile/6/f35d....jpg
                 */

                Path photoPath =
                        root.resolve(
                                photoDbPath
                        ).normalize();

//                System.out.println(
//                        "PDF PHOTO: "
//                                + photoPath.toAbsolutePath()
//                );

                if (Files.exists(photoPath)
                        && Files.isRegularFile(photoPath)) {

                    Image photo =
                            Image.getInstance(
                                    photoPath
                                            .toAbsolutePath()
                                            .toString()
                            );

                    // Bigger photo
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

                /*
                 * Example DB path:
                 *
                 * signature/6/e8c....png
                 *
                 * Actual:
                 *
                 * uploads/signature/6/e8c....png
                 */

                Path signaturePath =
                        root.resolve(
                                signatureDbPath
                        ).normalize();

//                System.out.println(
//                        "PDF SIGNATURE: "
//                                + signaturePath.toAbsolutePath()
//                );

                if (Files.exists(signaturePath)
                        && Files.isRegularFile(signaturePath)) {

                    Image signature =
                            Image.getInstance(
                                    signaturePath
                                            .toAbsolutePath()
                                            .toString()
                            );

                    // Bigger signature
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

        // Horizontal center
        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        // Vertical center
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