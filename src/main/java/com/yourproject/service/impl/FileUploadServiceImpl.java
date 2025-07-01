package com.yourproject.service.impl;

import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.FileNotFoundException; // Custom exception
import com.yourproject.service.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload-dir:./uploads}") // Default to ./uploads in current dir
    private String uploadDirString;

    private Path rootLocation;

    @Override
    @PostConstruct // Ensures this method is called after dependency injection is done
    public void init() {
        this.rootLocation = Paths.get(uploadDirString).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String... subDirectoryParts) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }
        // Generate a unique filename to prevent overwrites and issues with special chars
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path targetDirectory = this.rootLocation;
        if (subDirectoryParts != null && subDirectoryParts.length > 0) {
            for (String part : subDirectoryParts) {
                targetDirectory = targetDirectory.resolve(part);
            }
        }

        try {
            Files.createDirectories(targetDirectory); // Ensure subdirectory exists
            Path destinationFile = targetDirectory.resolve(uniqueFilename).normalize();

            if (!destinationFile.getParent().equals(targetDirectory)) {
                 throw new BadRequestException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return path relative to root upload directory for storage in DB
            return this.rootLocation.relativize(destinationFile).toString().replace("\\", "/");

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFilename, e);
        }
    }

    private Path buildPath(String filename, String... subDirectoryParts) {
        Path path = this.rootLocation;
        if (subDirectoryParts != null && subDirectoryParts.length > 0) {
            for (String part : subDirectoryParts) {
                path = path.resolve(part);
            }
        }
        return path.resolve(filename).normalize();
    }

    @Override
    public Path loadFile(String filename, String... subDirectoryParts) {
        return buildPath(filename, subDirectoryParts);
    }

    @Override
    public Resource loadFileAsResource(String filename, String... subDirectoryParts) {
        try {
            Path file = loadFile(filename, subDirectoryParts);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteFile(String filename, String... subDirectoryParts) {
         try {
            Path file = loadFile(filename, subDirectoryParts);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Log this error, but don't necessarily throw a fatal exception
            // if deleting a non-existent file is acceptable in some contexts.
            System.err.println("Error deleting file: " + filename + " - " + e.getMessage());
        }
    }
}
