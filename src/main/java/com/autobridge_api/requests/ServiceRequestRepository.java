package com.autobridge_api.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    Optional<ServiceRequest> findBySlotId(Long slotId);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
}
