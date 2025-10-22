package com.autobridge_api.vehicles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalVehicleImageStorage implements VehicleImageStorage {

    private final Path rootUploadDir;

    public LocalVehicleImageStorage(@Value("${autobridge.upload-dir:uploads}") String uploadDir) {
        this.rootUploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try { Files.createDirectories(rootUploadDir); } catch (IOException ignored) {}
    }

    @Override
    public String saveVehicleImage(Long vehicleId, MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        if (ext == null) ext = "jpg";
        String filename = UUID.randomUUID() + "." + ext.toLowerCase();
        Path dir = rootUploadDir.resolve("vehicles").resolve(String.valueOf(vehicleId));
        try {
            Files.createDirectories(dir);
            Path out = dir.resolve(filename);
            file.transferTo(out.toFile());
            // This URL is served by WebMvc config below
            return "/uploads/vehicles/" + vehicleId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }
    }

    @Override
    public void deleteAllForVehicle(Long vehicleId) {
        try {
            FileSystemUtils.deleteRecursively(rootUploadDir.resolve("vehicles").resolve(String.valueOf(vehicleId)));
        } catch (Exception ignored) {}
    }

    private static String extension(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : null;
    }
}
