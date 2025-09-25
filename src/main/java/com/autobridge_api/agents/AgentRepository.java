package com.autobridge_api.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByActiveTrue();
    List<Agent> findByActive(boolean active);
}
