package com.autobridge_api.vehicles;

/** Read-only projection for native query. */
public interface VehicleRow {
    Long getId();
    String getTitle();
    String getBrand();
    Double getPrice();
    String getImageUrl(); // column alias image_url AS imageUrl
}
