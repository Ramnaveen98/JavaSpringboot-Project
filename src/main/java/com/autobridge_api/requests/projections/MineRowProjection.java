// src/main/java/com/autobridge_api/requests/projections/MineRowProjection.java
package com.autobridge_api.requests.projections;

import com.autobridge_api.requests.RequestStatus;
import java.time.LocalDateTime;

public interface MineRowProjection {
    Long getId();
    String getServiceName();
    RequestStatus getStatus();        // enum from your entity
    LocalDateTime getSlotStartAtLocal();
    Long getInventoryVehicleId();
    String getAgentEmail();
}
