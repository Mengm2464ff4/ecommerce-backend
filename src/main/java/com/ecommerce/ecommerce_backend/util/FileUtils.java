// src/main/java/com/ecommerce/util/FileUtils.java
package com.ecommerce.ecommerce_backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class FileUtils {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            AppConstants.ALLOWED_IMAGE_EXTENSIONS.split(",")
    );

    private FileUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String generateFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public static boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    public static String saveFile(MultipartFile file, String uploadDir) throws IOException {
        if (!isValidImageFile(file)) {
            throw new IllegalArgumentException("Invalid image file");
        }

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = generateFileName(file);
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    public static boolean deleteFile(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                return true;
            }
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    public static long getFileSizeInMB(MultipartFile file) {
        return file.getSize() / (1024 * 1024);
    }

    public static boolean isFileSizeValid(MultipartFile file) {
        return file.getSize() <= AppConstants.MAX_FILE_SIZE;
    }
}