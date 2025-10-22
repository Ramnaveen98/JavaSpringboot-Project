package com.autobridge_api.requests;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<ServiceRequest, Long> {
}
