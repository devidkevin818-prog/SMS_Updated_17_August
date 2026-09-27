package com.bank.signaturemanagement.service;

import com.bank.signaturemanagement.entity.Employee;
import com.bank.signaturemanagement.entity.EmployeeSerialNumber;
import com.bank.signaturemanagement.repository.EmployeeRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ApprovedSignaturePdfService {

    // =========================================================
    // PDF Configuration
    // =========================================================

    private static final String PDF_HEADING =
            "AUTHORIZED SIGNATORY BOOK";

    /*
     * Final column order:
     *
     * 1. Employee Code
     * 2. Name
     * 3. Designation
     * 4. Department
     * 5. Branch Name
     * 6. Local Serial No.
     * 7. Foreign Serial No.
     * 8. Photo
     * 9. Signature
     * 10. Foreign Signature
     */
    private static final int TOTAL_COLUMNS = 10;

    private static final float DEFAULT_ROW_HEIGHT = 70f;

    private static final Font PAGE_TITLE_FONT =
            new Font(
                    Font.HELVETICA,
                    18f,
                    Font.BOLD
            );

    private static final Font TABLE_HEADER_FONT =
            new Font(
                    Font.HELVETICA,
                    6.5f,
                    Font.BOLD
            );

    private static final Font TABLE_CELL_FONT =
            new Font(
                    Font.HELVETICA,
                    6f,
                    Font.NORMAL
            );

    private static final Font IMAGE_MESSAGE_FONT =
            new Font(
                    Font.HELVETICA,
                    5.5f,
                    Font.BOLD
            );

    private static final Font PAGE_NUMBER_FONT =
            new Font(
                    Font.HELVETICA,
                    7f,
                    Font.NORMAL
            );

    private final EmployeeRepository employeeRepository;

    private final EmployeeSerialNumberService
            employeeSerialNumberService;

    // =========================================================
    // Storage Configuration
    // =========================================================
    //
    // application.properties:
    //
    // app.profile-photo.root=E:/images/employee-photo
    // app.signature.root=E:/images/employee-signature
    // app.logo.path=E:/images/logo/bank-logo.png
    //
    // =========================================================

    @Value("${app.profile-photo.root}")
    private String profilePhotoRoot;

    @Value("${app.signature.root}")
    private String signatureRoot;

    /*
     * The empty value after the colon makes the logo optional.
     * The application will start even when the property has
     * not been configured.
     */
    @Value("${app.logo.path:}")
    private String logoPath;

    public ApprovedSignaturePdfService(
            EmployeeRepository employeeRepository,
            EmployeeSerialNumberService employeeSerialNumberService
    ) {
        this.employeeRepository =
                employeeRepository;

        this.employeeSerialNumberService =
                employeeSerialNumberService;
    }

    // =========================================================
    // Generate Approved Signature PDF
    // =========================================================

    /*
     * The transaction remains active while the PDF accesses
     * lazy-loaded designation, department and branch values.
     */
    @Transactional(readOnly = true)
    public void generateApprovedPdf(
            OutputStream outputStream
    ) throws Exception {

        List<Employee> employees =
                employeeRepository.findAll();

        /*
         * Standard A4 portrait document.
         *
         * Width: approximately 595 points
         * Height: approximately 842 points
         *
         * The large top margin reserves space for the logo and
         * heading that repeat on every page.
         */
        Document document =
                new Document(
                        PageSize.A4,
                        18f,
                        18f,
                        115f,
                        30f
                );

        PdfWriter writer =
                PdfWriter.getInstance(
                        document,
                        outputStream
                );

        writer.setPageEvent(
                new SignatureBookPageEvent(
                        logoPath
                )
        );

        document.open();

        try {

            PdfPTable table =
                    createSignatureTable();

            if (employees == null
                    || employees.isEmpty()) {

                addEmptyTableMessage(table);

            } else {

                for (Employee employee : employees) {

                    addEmployeeRow(
                            table,
                            employee
                    );
                }
            }

            document.add(table);

        } finally {

            if (document.isOpen()) {
                document.close();
            }
        }
    }

    // =========================================================
    // Create Signature Table
    // =========================================================

    private PdfPTable createSignatureTable()
            throws Exception {

        PdfPTable table =
                new PdfPTable(TOTAL_COLUMNS);

        table.setWidthPercentage(100f);

        /*
         * Widths optimized for the requested ten columns on an
         * A4 portrait page.
         */
        table.setWidths(
                new float[]{
                        0.90f, // Employee Code
                        1.00f, // Name
                        1.00f, // Designation
                        1.00f, // Department
                        1.00f, // Branch Name
                        0.72f, // Local Serial No.
                        0.72f, // Foreign Serial No.
                        0.90f, // Photo
                        1.15f, // Signature
                        1.15f  // Foreign Signature
                }
        );

        table.setSplitRows(true);
        table.setSplitLate(false);
        table.setKeepTogether(false);

        /*
         * Repeat the table column headings on every page.
         */
        table.setHeaderRows(1);

        addHeader(table, "Employee Code");
        addHeader(table, "Name");
        addHeader(table, "Designation");
        addHeader(table, "Department");
        addHeader(table, "Branch Name");
        addHeader(table, "Local Serial No.");
        addHeader(table, "Foreign Serial No.");
        addHeader(table, "Photo");
        addHeader(table, "Signature");
        addHeader(table, "Foreign Signature");

        return table;
    }

    // =========================================================
    // Add Employee Row
    // =========================================================

    private void addEmployeeRow(
            PdfPTable table,
            Employee employee
    ) {

        if (employee == null) {
            return;
        }

        // =====================================================
        // Employee Code
        // =====================================================

        addCenteredCell(
                table,
                employee.getEmployeeNumber()
        );

        // =====================================================
        // Employee Name
        // =====================================================

        addCell(
                table,
                employee.getFullName()
        );

        // =====================================================
        // Designation
        // =====================================================

        String designationName = "";

        if (employee.getDesignation() != null) {

            designationName =
                    employee.getDesignation()
                            .getDesignationName();
        }

        addCell(
                table,
                designationName
        );

        // =====================================================
        // Department
        // =====================================================

        String departmentName = "";

        if (employee.getDepartment() != null) {

            departmentName =
                    employee.getDepartment()
                            .getDepartmentName();
        }

        addCell(
                table,
                departmentName
        );

        // =====================================================
        // Branch Name
        // =====================================================

        String branchName = "";

        if (employee.getBranch() != null) {

            branchName =
                    employee.getBranch()
                            .getBranchName();
        }

        addCell(
                table,
                branchName
        );

        // =====================================================
        // Latest Local and Foreign Serial Numbers
        // =====================================================

        String localSerialNumber = "";
        String foreignSerialNumber = "";

        if (employee.getId() != null) {

            EmployeeSerialNumber serialNumberRecord =
                    employeeSerialNumberService
                            .findLatestByEmployeeId(
                                    employee.getId()
                            )
                            .orElse(null);

            if (serialNumberRecord != null) {

                Integer localSerial =
                        serialNumberRecord
                                .getNewLocalSerial();

                Integer foreignSerial =
                        serialNumberRecord
                                .getNewForeignSerial();

                if (localSerial != null) {

                    localSerialNumber =
                            String.valueOf(
                                    localSerial
                            );
                }

                if (foreignSerial != null) {

                    foreignSerialNumber =
                            String.valueOf(
                                    foreignSerial
                            );
                }
            }
        }

        // =====================================================
        // Local Serial Number
        // =====================================================

        addCenteredCell(
                table,
                localSerialNumber
        );

        // =====================================================
        // Foreign Serial Number
        // =====================================================

        addCenteredCell(
                table,
                foreignSerialNumber
        );

        // =====================================================
        // Employee Photo
        // =====================================================

        table.addCell(
                createImageCell(
                        employee.getPhotoPath(),
                        ImageType.PHOTO
                )
        );

        // =====================================================
        // Local Signature
        // =====================================================

        table.addCell(
                createImageCell(
                        employee.getSignaturePath(),
                        ImageType.SIGNATURE
                )
        );

        // =====================================================
        // Foreign Signature
        // =====================================================

        table.addCell(
                createImageCell(
                        employee.getForeignSignaturePath(),
                        ImageType.FOREIGN_SIGNATURE
                )
        );
    }

    // =========================================================
    // Create Image Cell
    // =========================================================

    private PdfPCell createImageCell(
            String databasePath,
            ImageType imageType
    ) {

        PdfPCell cell =
                new PdfPCell();

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(3f);
        cell.setMinimumHeight(DEFAULT_ROW_HEIGHT);

        if (databasePath == null
                || databasePath.isBlank()) {

            addImageMessage(
                    cell,
                    imageType.getNotUploadedMessage()
            );

            return cell;
        }

        try {

            Path resolvedPath;

            if (imageType == ImageType.PHOTO) {

                resolvedPath =
                        resolvePhotoPath(
                                databasePath
                        );

            } else {

                resolvedPath =
                        resolveSignaturePath(
                                databasePath
                        );
            }

            if (!Files.exists(resolvedPath)
                    || !Files.isRegularFile(
                    resolvedPath
            )) {

                addImageMessage(
                        cell,
                        imageType.getNotFoundMessage()
                );

                return cell;
            }

            Image image =
                    Image.getInstance(
                            resolvedPath
                                    .toAbsolutePath()
                                    .toString()
                    );

            if (imageType == ImageType.PHOTO) {

                image.scaleToFit(
                        43f,
                        60f
                );

            } else {

                image.scaleToFit(
                        63f,
                        45f
                );
            }

            image.setAlignment(
                    Element.ALIGN_CENTER
            );

            cell.addElement(image);

        } catch (Exception exception) {

            addImageMessage(
                    cell,
                    imageType.getNotFoundMessage()
            );
        }

        return cell;
    }

    // =========================================================
    // Add Image Status Message
    // =========================================================

    private void addImageMessage(
            PdfPCell cell,
            String message
    ) {

        Paragraph paragraph =
                new Paragraph(
                        safeText(message),
                        IMAGE_MESSAGE_FONT
                );

        paragraph.setAlignment(
                Element.ALIGN_CENTER
        );

        paragraph.setSpacingBefore(20f);

        cell.addElement(paragraph);
    }

    // =========================================================
    // Add Empty Table Message
    // =========================================================

    private void addEmptyTableMessage(
            PdfPTable table
    ) {

        PdfPCell emptyCell =
                new PdfPCell(
                        new Phrase(
                                "No employee records found.",
                                TABLE_CELL_FONT
                        )
                );

        emptyCell.setColspan(
                TOTAL_COLUMNS
        );

        emptyCell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        emptyCell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        emptyCell.setPadding(15f);

        table.addCell(emptyCell);
    }

    // =========================================================
    // Resolve Photo Path
    // =========================================================

    private Path resolvePhotoPath(
            String databasePath
    ) {

        String relativePath =
                normalizeDatabasePath(
                        databasePath
                );

        /*
         * Database:
         *
         * profile/1/photo.jpg
         *
         * Actual file:
         *
         * E:/images/employee-photo/1/photo.jpg
         */
        if (relativePath.startsWith(
                "profile/"
        )) {

            relativePath =
                    relativePath.substring(
                            "profile/".length()
                    );
        }

        Path root =
                Paths.get(profilePhotoRoot)
                        .toAbsolutePath()
                        .normalize();

        Path resolvedPath =
                root.resolve(relativePath)
                        .normalize();

        verifyPathWithinRoot(
                root,
                resolvedPath
        );

        return resolvedPath;
    }

    // =========================================================
    // Resolve Signature Path
    // =========================================================

    private Path resolveSignaturePath(
            String databasePath
    ) {

        String relativePath =
                normalizeDatabasePath(
                        databasePath
                );

        /*
         * Supported database path formats:
         *
         * signature/1/signature.png
         * foreign-signature/1/signature.png
         * foreign_signature/1/signature.png
         */
        if (relativePath.startsWith(
                "signature/"
        )) {

            relativePath =
                    relativePath.substring(
                            "signature/".length()
                    );

        } else if (relativePath.startsWith(
                "foreign-signature/"
        )) {

            relativePath =
                    relativePath.substring(
                            "foreign-signature/".length()
                    );

        } else if (relativePath.startsWith(
                "foreign_signature/"
        )) {

            relativePath =
                    relativePath.substring(
                            "foreign_signature/".length()
                    );
        }

        Path root =
                Paths.get(signatureRoot)
                        .toAbsolutePath()
                        .normalize();

        Path resolvedPath =
                root.resolve(relativePath)
                        .normalize();

        verifyPathWithinRoot(
                root,
                resolvedPath
        );

        return resolvedPath;
    }

    // =========================================================
    // Normalize Database Path
    // =========================================================

    private String normalizeDatabasePath(
            String databasePath
    ) {

        String normalizedPath =
                databasePath == null
                        ? ""
                        : databasePath.trim();

        /*
         * Convert Windows-style separators to forward slashes.
         */
        normalizedPath =
                normalizedPath.replace(
                        '\\',
                        '/'
                );

        while (normalizedPath.startsWith("/")) {

            normalizedPath =
                    normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    // =========================================================
    // Verify Resolved Path
    // =========================================================

    private void verifyPathWithinRoot(
            Path root,
            Path resolvedPath
    ) {

        if (!resolvedPath.startsWith(root)) {

            throw new IllegalArgumentException(
                    "Invalid file path outside configured root: "
                            + resolvedPath
            );
        }
    }

    // =========================================================
    // Add Header Cell
    // =========================================================

    private void addHeader(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safeText(text),
                                TABLE_HEADER_FONT
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(3f);
        cell.setMinimumHeight(32f);

        table.addCell(cell);
    }

    // =========================================================
    // Add Normal Cell
    // =========================================================

    private void addCell(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safeText(text),
                                TABLE_CELL_FONT
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(3f);
        cell.setMinimumHeight(DEFAULT_ROW_HEIGHT);

        table.addCell(cell);
    }

    // =========================================================
    // Add Centered Cell
    // =========================================================

    private void addCenteredCell(
            PdfPTable table,
            String text
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safeText(text),
                                TABLE_CELL_FONT
                        )
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(3f);
        cell.setMinimumHeight(DEFAULT_ROW_HEIGHT);

        table.addCell(cell);
    }

    // =========================================================
    // Null-Safe Text
    // =========================================================

    private String safeText(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    // =========================================================
    // Image Type
    // =========================================================

    private enum ImageType {

        PHOTO(
                "Photo not uploaded",
                "Photo not found"
        ),

        SIGNATURE(
                "Signature not uploaded",
                "Signature not found"
        ),

        FOREIGN_SIGNATURE(
                "Foreign signature not uploaded",
                "Foreign signature not found"
        );

        private final String notUploadedMessage;
        private final String notFoundMessage;

        ImageType(
                String notUploadedMessage,
                String notFoundMessage
        ) {
            this.notUploadedMessage =
                    notUploadedMessage;

            this.notFoundMessage =
                    notFoundMessage;
        }

        public String getNotUploadedMessage() {
            return notUploadedMessage;
        }

        public String getNotFoundMessage() {
            return notFoundMessage;
        }
    }

    // =========================================================
    // Repeating Page Header
    // =========================================================

    private static class SignatureBookPageEvent
            extends PdfPageEventHelper {

        /*
         * Move the complete header away from the page corners.
         */
        private static final float HEADER_SIDE_INSET =
                30f;

        private final String configuredLogoPath;

        private SignatureBookPageEvent(
                String configuredLogoPath
        ) {
            this.configuredLogoPath =
                    configuredLogoPath;
        }

        @Override
        public void onEndPage(
                PdfWriter writer,
                Document document
        ) {

            try {

                addRepeatingHeader(
                        writer,
                        document
                );

                addPageNumber(
                        writer,
                        document
                );

            } catch (Exception exception) {

                /*
                 * A logo or header problem should not interrupt
                 * PDF generation.
                 */
                exception.printStackTrace();
            }
        }

        // =====================================================
        // Add Repeating Logo and Heading
        // =====================================================

        private void addRepeatingHeader(
                PdfWriter writer,
                Document document
        ) throws Exception {

            PdfPTable headerTable =
                    new PdfPTable(3);

            /*
             * The complete header is narrower than the page
             * content. This prevents the logo from being placed
             * too close to the top-left corner.
             */
            float headerWidth =
                    document.getPageSize().getWidth()
                            - document.leftMargin()
                            - document.rightMargin()
                            - (HEADER_SIDE_INSET * 2f);

            headerTable.setTotalWidth(
                    headerWidth
            );

            headerTable.setLockedWidth(true);

            /*
             * Equal left and right columns ensure that the
             * heading remains visually centered.
             */
            headerTable.setWidths(
                    new float[]{
                            2.2f,
                            5.6f,
                            2.2f
                    }
            );

            PdfPCell logoCell =
                    createLogoCell();

            PdfPCell titleCell =
                    new PdfPCell(
                            new Phrase(
                                    PDF_HEADING,
                                    PAGE_TITLE_FONT
                            )
                    );

            titleCell.setBorder(
                    Rectangle.NO_BORDER
            );

            titleCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            titleCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            titleCell.setPaddingTop(18f);
            titleCell.setPaddingBottom(18f);

            /*
             * Balance the logo column so the heading remains
             * centered on the full page.
             */
            PdfPCell balanceCell =
                    new PdfPCell(
                            new Phrase("")
                    );

            balanceCell.setBorder(
                    Rectangle.NO_BORDER
            );

            balanceCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            headerTable.addCell(logoCell);
            headerTable.addCell(titleCell);
            headerTable.addCell(balanceCell);

            float xPosition =
                    document.leftMargin()
                            + HEADER_SIDE_INSET;

            float yPosition =
                    document.getPageSize().getHeight()
                            - 18f;

            headerTable.writeSelectedRows(
                    0,
                    -1,
                    xPosition,
                    yPosition,
                    writer.getDirectContent()
            );
        }

        // =====================================================
        // Create Logo Cell
        // =====================================================

        private PdfPCell createLogoCell() {

            PdfPCell logoCell =
                    new PdfPCell();

            logoCell.setBorder(
                    Rectangle.NO_BORDER
            );

            logoCell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            logoCell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE
            );

            logoCell.setPaddingLeft(8f);
            logoCell.setPaddingRight(8f);
            logoCell.setPaddingTop(4f);
            logoCell.setPaddingBottom(4f);

            if (configuredLogoPath == null
                    || configuredLogoPath.isBlank()) {

                return logoCell;
            }

            try {

                Path logoFile =
                        Paths.get(configuredLogoPath)
                                .toAbsolutePath()
                                .normalize();

                if (!Files.exists(logoFile)
                        || !Files.isRegularFile(
                        logoFile
                )) {

                    return logoCell;
                }

                Image logo =
                        Image.getInstance(
                                logoFile.toString()
                        );

                /*
                 * Large logo suitable for an A4 portrait page.
                 * The original aspect ratio is preserved.
                 */
                logo.scaleToFit(
                        90f,
                        75f
                );

                logo.setAlignment(
                        Element.ALIGN_CENTER
                );

                logoCell.addElement(logo);

            } catch (Exception ignored) {

                /*
                 * Generate the remaining PDF even when the logo
                 * cannot be loaded.
                 */
            }

            return logoCell;
        }

        // =====================================================
        // Add Page Number
        // =====================================================

        private void addPageNumber(
                PdfWriter writer,
                Document document
        ) {

            Phrase pageNumber =
                    new Phrase(
                            "Page "
                                    + writer.getPageNumber(),
                            PAGE_NUMBER_FONT
                    );

            float xPosition =
                    document.getPageSize().getWidth()
                            - document.rightMargin();

            float yPosition =
                    document.bottomMargin() - 12f;

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    pageNumber,
                    xPosition,
                    yPosition,
                    0f
            );
        }
    }
}
