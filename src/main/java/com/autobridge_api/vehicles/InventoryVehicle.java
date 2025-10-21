package com.autobridge_api.vehicles;

import jakarta.persistence.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_vehicles")
public class InventoryVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional VIN. Unique if present so seeding/upserts can key on it. */
    @Column(unique = true, length = 32)
    private String vin;

    /** Primary marketing title, e.g. "Toyota Prius". */
    @Column(nullable = false)
    private String title;

    /** Make/brand, e.g. "Toyota". */
    @Column(nullable = false)
    private String brand;

    /** Optional model trim, e.g. "Prius" or "EX-L". */
    @Column(length = 64)
    private String model;

    /** Display color (e.g., "Blue", "Midnight Black"). */
    @Column(length = 64)
    private String color;

    private Integer year;

    /** Monetary – BigDecimal with 2 decimals. */
    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    /** Inventory state (AVAILABLE, PENDING, RESERVED, SOLD) */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private InventoryStatus status;

    /** Absolute or relative URL (e.g., "/uploads/vehicles/1/x.jpg" or https://...) */
    @Column(length = 2048)
    private String imageUrl;

    /** Marketing / details text (nullable). */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() { this.updatedAt = Instant.now(); }

    // ----- getters/setters -----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    /** Alias compatibility for legacy code using "name" */
    public String getName() { return getTitle(); }
    public void setName(String name) { setTitle(name); }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    /** Alias compatibility for legacy code using "make" */
    public String getMake() { return getBrand(); }
    public void setMake(String make) { setBrand(make); }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /* ================================================================================
       Builder compatible with seeders and legacy code:
         InventoryVehicle.builder()
           .vin("...")
           .make(VehicleMake or String)   // -> brand
           .model(VehicleModel or String) // -> model
           .color("Blue" or enum)
           .year(2025)
           .price(new BigDecimal("25990.00")) or .price(25990)
           .status(InventoryStatus or "AVAILABLE")
           .imageUrl("...")
           .description("…")
           .title("Toyota Prius") // optional; else we compose from brand+model
           .build();
       ================================================================================ */
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String vin;
        private String title;
        private String brand;
        private String model;
        private String color;
        private Integer year;
        private BigDecimal price;
        private InventoryStatus status;
        private String imageUrl;
        private String description;

        private Builder() {}

        public Builder vin(String vin) { this.vin = vin; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }

        /** Accept whatever VehicleMake is (enum/class). Prefer getName()/getDisplayName(), else toString(). */
        public Builder make(com.autobridge_api.vehicles.VehicleMake make) {
            this.brand = toNiceString(make); return this;
        }
        /** Also accept plain string for make->brand. */
        public Builder make(String make) { this.brand = make; return this; }

        public Builder model(String model) { this.model = model; return this; }
        public Builder model(com.autobridge_api.vehicles.VehicleModel model) {
            this.model = toNiceString(model); return this;
        }

        public Builder color(String color) { this.color = color; return this; }
        public Builder color(Object colorObj) { this.color = toNiceString(colorObj); return this; }

        public Builder year(Integer year) { this.year = year; return this; }

        /** BigDecimal price (preferred). */
        public Builder price(BigDecimal price) { this.price = price; return this; }
        /** Overload: integer → BigDecimal. */
        public Builder price(Integer price) {
            this.price = (price == null) ? null : new BigDecimal(price.toString());
            return this;
        }

        public Builder status(InventoryStatus status) { this.status = status; return this; }
        public Builder status(String status) {
            if (status == null || status.isBlank()) { this.status = null; return this; }
            try { this.status = InventoryStatus.valueOf(status.trim().toUpperCase()); }
            catch (Exception ignored) {}
            return this;
        }

        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public InventoryVehicle build() {
            InventoryVehicle v = new InventoryVehicle();
            v.setVin(this.vin);

            // Compose title if missing
            String finalTitle = this.title;
            if (isBlank(finalTitle)) {
                if (!isBlank(this.brand) && !isBlank(this.model)) finalTitle = this.brand + " " + this.model;
                else if (!isBlank(this.model)) finalTitle = this.model;
                else if (!isBlank(this.brand)) finalTitle = this.brand;
            }
            v.setTitle(!isBlank(finalTitle) ? finalTitle : "Vehicle");

            v.setBrand(this.brand != null ? this.brand : "");
            v.setModel(this.model);
            v.setColor(this.color);
            v.setYear(this.year);
            v.setPrice(this.price);
            v.setStatus(this.status);
            v.setImageUrl(this.imageUrl);
            v.setDescription(this.description);
            return v;
        }

        private static boolean isBlank(String s){ return s==null || s.isBlank(); }

        /** Try getName()/getDisplayName() via reflection; else use toString(). */
        private static String toNiceString(Object obj) {
            if (obj == null) return null;
            try {
                Method m = obj.getClass().getMethod("getName");
                Object r = m.invoke(obj);
                if (r != null) return String.valueOf(r);
            } catch (Throwable ignored) {}
            try {
                Method m = obj.getClass().getMethod("getDisplayName");
                Object r = m.invoke(obj);
                if (r != null) return String.valueOf(r);
            } catch (Throwable ignored) {}
            return String.valueOf(obj); // enum.name() or class toString()
        }
    }
}
