package com.autobridge_api.slots;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long>, JpaSpecificationExecutor<Slot> {


    List<Slot> findByTypeAndStatusAndStartAtBetween(
            SlotType type,
            SlotStatus status,
            Instant from,
            Instant to
    );


    List<Slot> findByTypeAndStatusAndStartAtBetweenAndAgentId(
            SlotType type,
            SlotStatus status,
            Instant from,
            Instant to,
            Long agentId
    );
}
