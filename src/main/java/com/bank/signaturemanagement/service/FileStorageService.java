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
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private final Path uploadRoot;
    private final Path profilePhotoRoot;
    private final Path signatureRoot;

    public FileStorageService(@Value("${app.upload.root}") String uploadRoot,
                              @Value("${app.profile-photo.root}") String profilePhotoRoot,
                              @Value("${app.signature.root}") String signatureRoot) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
        this.profilePhotoRoot = Path.of(profilePhotoRoot).toAbsolutePath().normalize();
        this.signatureRoot = Path.of(signatureRoot).toAbsolutePath().normalize();
    }

    public String storeEmployeeImage(MultipartFile file, String imageType, Long employeeId) {
        validateImage(file);
        Path root = rootFor(imageType);
        String extension = file.getContentType().equalsIgnoreCase("image/png") ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        Path employeeFolder = root.resolve(employeeId.toString()).normalize();
        requireInsideRoot(employeeFolder, root);
        try {
            Files.createDirectories(employeeFolder);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, employeeFolder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return imageType + "/" + employeeId + "/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save the uploaded image", exception);
        }
    }

    public String organizeEmployeeImage(String storedPath, String imageType, Long employeeId) {
        String expectedPrefix = imageType + "/" + employeeId + "/";
        if (storedPath.startsWith(expectedPrefix)) return storedPath;

        Path source = resolveStoredPath(storedPath);
        if (!Files.isRegularFile(source)) {
            throw new IllegalStateException("Stored employee image was not found: " + storedPath);
        }
        String fileName = source.getFileName().toString();
        Path root = rootFor(imageType);
        Path employeeFolder = root.resolve(employeeId.toString()).normalize();
        requireInsideRoot(employeeFolder, root);
        Path destination = employeeFolder.resolve(fileName).normalize();
        requireInsideRoot(destination, root);
        try {
            Files.createDirectories(employeeFolder);
            if (storedPath.startsWith("employee-photo/")
                    || storedPath.startsWith("employee-signature/")
                    || storedPath.startsWith("pending-photo/")
                    || storedPath.startsWith("pending-signature/")) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return expectedPrefix + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not organize the employee image", exception);
        }
    }

    public void deletePendingImage(String storedPath) {
        if (storedPath == null || !(storedPath.startsWith("employee-photo/")
                || storedPath.startsWith("employee-signature/")
                || storedPath.startsWith("pending-photo/")
                || storedPath.startsWith("pending-signature/"))) return;
        Path path = uploadRoot.resolve(storedPath).normalize();
        requireInsideRoot(path, uploadRoot);
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove the rejected pending image", exception);
        }
    }

    private Path resolveStoredPath(String storedPath) {
        Path root;
        String relative;
        if (storedPath.startsWith("profile/")) {
            root = profilePhotoRoot;
            relative = storedPath.substring("profile/".length());
        } else if (storedPath.startsWith("signature/")) {
            root = signatureRoot;
            relative = storedPath.substring("signature/".length());
        } else {
            root = uploadRoot;
            relative = storedPath;
        }
        Path resolved = root.resolve(relative).normalize();
        requireInsideRoot(resolved, root);
        return resolved;
    }

    private Path rootFor(String imageType) {
        if ("profile".equals(imageType)) return profilePhotoRoot;
        if ("signature".equals(imageType)) return signatureRoot;
        throw new IllegalArgumentException("Invalid employee image type");
    }

    private void requireInsideRoot(Path path, Path root) {
        if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid image path");
    }

    public String storeImage(MultipartFile file, String folder) {
        validateImage(file);
        String contentType = file.getContentType();
        String extension = contentType.equalsIgnoreCase("image/png") ? ".png" : ".jpg";
        String fileName = UUID.randomUUID() + extension;
        Path folderPath = uploadRoot.resolve(folder).normalize();
        if (!folderPath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid upload folder");
        }
        try {
            Files.createDirectories(folderPath);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, folderPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return folder + "/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save the uploaded image", exception);
        }
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPG and PNG images are allowed");
        }
        try (var inputStream = file.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new IllegalArgumentException("The uploaded file is not a valid image");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read the uploaded image", exception);
        }
    }

    public Path getUploadRoot() { return uploadRoot; }
    public Path getProfilePhotoRoot() { return profilePhotoRoot; }
    public Path getSignatureRoot() { return signatureRoot; }
}
