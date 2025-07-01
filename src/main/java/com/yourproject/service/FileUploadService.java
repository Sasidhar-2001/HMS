package com.yourproject.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import org.springframework.core.io.Resource;

public interface FileUploadService {
    /**
     * Stores a file.
     * @param file The file to store.
     * @param subDirectory Optional subdirectory within the main upload directory.
     * @return The path (relative to upload root) where the file was stored.
     */
    String storeFile(MultipartFile file, String... subDirectory);

    Path loadFile(String filename, String... subDirectory);

    Resource loadFileAsResource(String filename, String... subDirectory);

    void deleteFile(String filename, String... subDirectory);

    void init(); // To create root directory
}
