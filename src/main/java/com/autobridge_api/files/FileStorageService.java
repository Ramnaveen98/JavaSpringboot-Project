package com.autobridge_api.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

@Service
public class FileStorageService {
    private final Path rootDir;

    public FileStorageService(@Value("${autobridge.upload-dir:uploads}") String root) throws IOException {
        this.rootDir = Paths.get(root).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    public String saveVehicleImage(Long vehicleId, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String stamped = Instant.now().toEpochMilli() + "_" + safe;

        Path vehicleDir = rootDir.resolve("vehicles").resolve(String.valueOf(vehicleId));
        Files.createDirectories(vehicleDir);

        Path dest = vehicleDir.resolve(stamped).normalize();
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/vehicles/" + vehicleId + "/" + stamped; // served by WebConfig
    }
}
