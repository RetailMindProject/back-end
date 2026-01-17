package com.example.back_end.modules.messages.service;

import com.example.back_end.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    // الأنواع المسموح بها
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    // الحد الأقصى لحجم الملف: 5 MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public FileStorageService(@Value("${file.upload-dir:uploads/messages}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new CustomException("Could not create the directory where the uploaded files will be stored.");
        }
    }

    public String storeFile(MultipartFile file) {
        // التحقق من الملف
        validateFile(file);

        try {
            // إنشاء اسم ملف فريد باستخدام امتداد آمن بناءً على نوع MIME المعتمد
            // لا نستخدم اسم الملف الأصلي لتجنب ثغرات Path Traversal
            String safeExtension = getSafeExtensionFromMimeType(file.getContentType());
            String newFileName = UUID.randomUUID().toString() + safeExtension;

            // نسخ الملف إلى الموقع المستهدف
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", newFileName);

            return newFileName;

        } catch (IOException ex) {
            log.error("Could not store file. Error: {}", ex.getMessage());
            throw new CustomException("Could not store file. Please try again!");
        }
    }

    public String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = digest.digest(fileBytes);

            // تحويل إلى hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error calculating checksum: {}", e.getMessage());
            return null;
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("File deleted successfully: {}", fileName);
        } catch (IOException ex) {
            log.error("Could not delete file {}. Error: {}", fileName, ex.getMessage());
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("Cannot upload empty file");
        }

        // التحقق من نوع الملف
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new CustomException("File type not allowed. Allowed types: PDF, Excel, Images (JPEG, PNG, GIF)");
        }

        // التحقق من حجم الملف
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException("File size exceeds maximum limit of 5MB");
        }
    }

    /**
     * Get safe file extension based on validated MIME type.
     * This prevents path traversal attacks by not using user-provided filenames.
     */
    private String getSafeExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }

        return switch (mimeType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }
}