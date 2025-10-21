package com.autobridge_api.vehicles;

import org.springframework.web.multipart.MultipartFile;

public interface VehicleImageStorage {
    /** Save file for a vehicle and return the public URL that frontend should use. */
    String saveVehicleImage(Long vehicleId, MultipartFile file);
    /** Best-effort cleanup */
    void deleteAllForVehicle(Long vehicleId);
}
