package com.bank.signaturemanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png");

    private final Path uploadRoot;
    private final Path profilePhotoRoot;
    private final Path signatureRoot;
    private final Path foreignSignatureRoot;

    public FileStorageService(
            @Value("${app.upload.root}") String uploadRoot,
            @Value("${app.profile-photo.root}") String profilePhotoRoot,
            @Value("${app.signature.root}") String signatureRoot,
            @Value("${app.foreign-signature.root}") String foreignSignatureRoot) {

        this.uploadRoot = Path.of(uploadRoot)
                .toAbsolutePath()
                .normalize();

        this.profilePhotoRoot = Path.of(profilePhotoRoot)
                .toAbsolutePath()
                .normalize();

        this.signatureRoot = Path.of(signatureRoot)
                .toAbsolutePath()
                .normalize();

        this.foreignSignatureRoot = Path.of(foreignSignatureRoot)
                .toAbsolutePath()
                .normalize();
    }


    // =========================================================
    // EMPLOYEE IMAGE STORAGE
    // =========================================================

    public String storeEmployeeImage(
            MultipartFile file,
            String imageType,
            Long employeeId) {

        validateImage(file);

        Path root = rootFor(imageType);

        String extension =
                file.getContentType().equalsIgnoreCase("image/png")
                        ? ".png"
                        : ".jpg";

        String fileName = UUID.randomUUID() + extension;

        Path employeeFolder = root
                .resolve(employeeId.toString())
                .normalize();

        requireInsideRoot(employeeFolder, root);

        try {

            Files.createDirectories(employeeFolder);

            try (var inputStream = file.getInputStream()) {

                Files.copy(
                        inputStream,
                        employeeFolder.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return imageType + "/" + employeeId + "/" + fileName;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not save the uploaded image",
                    exception
            );
        }
    }


    // =========================================================
    // ORGANIZE EMPLOYEE IMAGE
    // =========================================================

    public String organizeEmployeeImage(
            String storedPath,
            String imageType,
            Long employeeId) {

        String expectedPrefix =
                imageType + "/" + employeeId + "/";

        if (storedPath.startsWith(expectedPrefix)) {
            return storedPath;
        }

        Path source = resolveStoredPath(storedPath);

        if (!Files.isRegularFile(source)) {

            throw new IllegalStateException(
                    "Stored employee image was not found: "
                            + storedPath
            );
        }

        String fileName =
                source.getFileName().toString();

        Path root = rootFor(imageType);

        Path employeeFolder = root
                .resolve(employeeId.toString())
                .normalize();

        requireInsideRoot(employeeFolder, root);

        Path destination = employeeFolder
                .resolve(fileName)
                .normalize();

        requireInsideRoot(destination, root);

        try {

            Files.createDirectories(employeeFolder);

            if (storedPath.startsWith("employee-photo/")
                    || storedPath.startsWith("employee-signature/")
                    || storedPath.startsWith("pending-photo/")
                    || storedPath.startsWith("pending-signature/")
                    || storedPath.startsWith("pending-foreign-signature/")) {

                Files.move(
                        source,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } else {

                Files.copy(
                        source,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return expectedPrefix + fileName;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not organize the employee image",
                    exception
            );
        }
    }


    // =========================================================
    // DELETE PENDING IMAGE
    // =========================================================

    public void deletePendingImage(String storedPath) {

        if (storedPath == null
                || !(storedPath.startsWith("employee-photo/")
                || storedPath.startsWith("employee-signature/")
                || storedPath.startsWith("pending-photo/")
                || storedPath.startsWith("pending-signature/")
                || storedPath.startsWith("pending-foreign-signature/"))) {

            return;
        }

        Path path = uploadRoot
                .resolve(storedPath)
                .normalize();

        requireInsideRoot(path, uploadRoot);

        try {
            if (!Files.exists(path)) return;
            Path archiveRoot = uploadRoot.resolve("archive/rejected").normalize();
            requireInsideRoot(archiveRoot, uploadRoot);
            Files.createDirectories(archiveRoot);
            Path archived = archiveRoot.resolve(UUID.randomUUID() + "-" + path.getFileName()).normalize();
            requireInsideRoot(archived, archiveRoot);
            Files.move(path, archived, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not archive the rejected pending image",
                    exception
            );
        }
    }


    // =========================================================
    // RESOLVE STORED PATH
    // =========================================================

    private Path resolveStoredPath(String storedPath) {

        Path root;
        String relative;

        if (storedPath.startsWith("profile/")) {

            root = profilePhotoRoot;

            relative = storedPath.substring(
                    "profile/".length()
            );

        } else if (storedPath.startsWith("signature/")) {

            root = signatureRoot;

            relative = storedPath.substring(
                    "signature/".length()
            );

        } else if (storedPath.startsWith("foreign-signature/")) {

            root = foreignSignatureRoot;

            relative = storedPath.substring(
                    "foreign-signature/".length()
            );

        } else {

            root = uploadRoot;

            relative = storedPath;
        }

        Path resolved = root
                .resolve(relative)
                .normalize();

        requireInsideRoot(resolved, root);

        return resolved;
    }

    public Path resolveForRead(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || storedPath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid media path");
        }
        return resolveStoredPath(storedPath.replace('\\','/'));
    }


    // =========================================================
    // ROOT FOR IMAGE TYPE
    // =========================================================

    private Path rootFor(String imageType) {

        if ("profile".equals(imageType)) {
            return profilePhotoRoot;
        }

        if ("signature".equals(imageType)) {
            return signatureRoot;
        }

        if ("foreign-signature".equals(imageType)) {
            return foreignSignatureRoot;
        }

        throw new IllegalArgumentException(
                "Invalid employee image type"
        );
    }


    // =========================================================
    // PATH SECURITY
    // =========================================================

    private void requireInsideRoot(
            Path path,
            Path root) {

        if (!path.startsWith(root)) {

            throw new IllegalArgumentException(
                    "Invalid image path"
            );
        }
    }


    // =========================================================
    // GENERIC IMAGE STORAGE
    // =========================================================

    public String storeImage(
            MultipartFile file,
            String folder) {

        validateImage(file);

        String contentType =
                file.getContentType();

        String extension =
                contentType.equalsIgnoreCase("image/png")
                        ? ".png"
                        : ".jpg";

        String fileName =
                UUID.randomUUID() + extension;

        Path folderPath = uploadRoot
                .resolve(folder)
                .normalize();

        if (!folderPath.startsWith(uploadRoot)) {

            throw new IllegalArgumentException(
                    "Invalid upload folder"
            );
        }

        try {

            Files.createDirectories(folderPath);

            try (var inputStream =
                         file.getInputStream()) {

                Files.copy(
                        inputStream,
                        folderPath.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return folder + "/" + fileName;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not save the uploaded image",
                    exception
            );
        }
    }

    public String storeImportFile(MultipartFile file, String batchNumber) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Select a CSV or XLSX file");
        String original=file.getOriginalFilename()==null?"":file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!original.endsWith(".csv") && !original.endsWith(".xlsx")) throw new IllegalArgumentException("Only CSV and XLSX files are allowed");
        if (file.getSize()>10L*1024*1024) throw new IllegalArgumentException("Batch file must not exceed 10 MB");
        Path folder=uploadRoot.resolve("import-batches").normalize(); requireInsideRoot(folder,uploadRoot);
        String extension=original.endsWith(".xlsx")?".xlsx":".csv";
        Path destination=folder.resolve(batchNumber+"-"+UUID.randomUUID()+extension).normalize(); requireInsideRoot(destination,folder);
        try { Files.createDirectories(folder); try(var in=file.getInputStream()){Files.copy(in,destination,StandardCopyOption.REPLACE_EXISTING);} }
        catch(IOException e){throw new IllegalStateException("Could not retain batch source file",e);}
        return "import-batches/"+destination.getFileName();
    }


    // =========================================================
    // IMAGE VALIDATION
    // =========================================================

    public void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select an image file"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT))) {

            throw new IllegalArgumentException(
                    "Only JPG and PNG images are allowed"
            );
        }

        try (var inputStream =
                     file.getInputStream()) {

            if (ImageIO.read(inputStream) == null) {

                throw new IllegalArgumentException(
                        "The uploaded file is not a valid image"
                );
            }

        } catch (IOException exception) {

            throw new IllegalArgumentException(
                    "Could not read the uploaded image",
                    exception
            );
        }
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public Path getProfilePhotoRoot() {
        return profilePhotoRoot;
    }

    public Path getSignatureRoot() {
        return signatureRoot;
    }

    public Path getForeignSignatureRoot() {
        return foreignSignatureRoot;
    }
}
