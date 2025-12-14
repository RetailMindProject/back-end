package com.example.back_end.modules.catalog.product.service;

import com.example.back_end.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private final Path baseStorage;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    public ImageStorageService(@Value("${file.product-image-dir:uploads/products}") String dir) {
        this.baseStorage = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorage);
        } catch (Exception ex) {
            throw new CustomException("Could not create product image upload directory");
        }
    }

    public String store(Long productId, MultipartFile file) {
        validate(file);
        Path productDir = getProductDirectory(productId);
        
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = getExtension(original);
        String name = UUID.randomUUID() + ext;
        Path target = productDir.resolve(name);
        
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored image {} for product {}", name, productId);
            return name;
        } catch (IOException e) {
            log.error("Failed to store image for product {}: {}", productId, e.getMessage());
            throw new CustomException("Could not store image. Try again.");
        }
    }

    public Path getPath(Long productId, String fileName) {
        return getProductDirectory(productId).resolve(fileName).normalize();
    }

    public void delete(Long productId, String fileName) {
        try {
            Path filePath = getPath(productId, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted image {} for product {}", fileName, productId);
            }
        } catch (IOException e) {
            log.error("Failed to delete image {} for product {}: {}", fileName, productId, e.getMessage());
            throw new CustomException("Could not delete image");
        }
    }

    public String buildUrl(Long productId, String fileName) {
        return "/api/products/" + productId + "/images/" + fileName;
    }

    private Path getProductDirectory(Long productId) {
        Path productDir = baseStorage.resolve(String.valueOf(productId));
        try {
            Files.createDirectories(productDir);
        } catch (IOException e) {
            throw new CustomException("Could not create product directory");
        }
        return productDir;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("File is empty");
        }
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new CustomException("Unsupported image type");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new CustomException("Image exceeds 5MB limit");
        }
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx == -1 ? "" : name.substring(idx);
    }
}

