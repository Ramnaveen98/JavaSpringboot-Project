// src/main/java/com/autobridge_api/requests/ServiceRequestRepository.java
package com.autobridge_api.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime; // <-- match your entity's createdAt type
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    Optional<ServiceRequest> findBySlotId(Long slotId);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    // ---- Admin table ----
    @Query("""
        select r.id as id,
               coalesce(s.name, 'Unknown') as serviceName,
               r.status as status,
               null as assignedAgentName
        from ServiceRequest r
        left join r.service s
        order by r.createdAt desc
    """)
    List<RequestRowProjection> findAllAdminRows();

    interface RequestRowProjection {
        Long getId();
        String getServiceName();
        RequestStatus getStatus();
        String getAssignedAgentName();
    }

    // ---- USER "mine" rows (projection only) ----
    @Query("""
        select r.id as id,
               coalesce(s.name, 'Unknown') as serviceName,
               r.status as status,
               r.createdAt as slotStartAtLocal,
               v.id as inventoryVehicleId,
               a.email as agentEmail
        from ServiceRequest r
        left join r.service s
        left join r.assignedAgent a
        left join r.inventoryVehicle v
        where lower(r.userEmail) = lower(:email)
        order by r.id desc
    """)
    List<MineRowProjection> findMineRows(String email);

    interface MineRowProjection {
        Long getId();
        String getServiceName();
        RequestStatus getStatus();
        LocalDateTime getSlotStartAtLocal(); // use the SAME type as ServiceRequest.createdAt
        Long getInventoryVehicleId();
        String getAgentEmail();
    }
}
