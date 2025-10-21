package com.autobridge_api.agents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Agent.
 *
 * NOTE:
 * - The Agent entity does NOT have a userId field.
 * - To keep compatibility with existing code that calls findByUserId(...),
 *   we resolve it by joining the canonical `users` table using email
 *   (case-insensitive match).
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /** Common lookups */
    List<Agent> findByActiveTrue();
    List<Agent> findByActive(boolean active);

    Optional<Agent> findByEmailIgnoreCase(String email);

    /** Alias kept for older code that expects this name */
    List<Agent> findAllByActiveTrue();

    /**
     * Backward-compatible method: find Agent by canonical user id.
     * Since Agent has no userId property, we join on LOWER(email).
     *
     * Tables assumed:
     *   - users  (canonical accounts, has columns: id, email, role, ...)
     *   - agents (operational agent profile, has columns: id, email, active, ...)
     */
    @Query(value = """
        SELECT a.*
          FROM agents a
          JOIN users u ON LOWER(a.email) = LOWER(u.email)
         WHERE u.id = :userId
         LIMIT 1
        """, nativeQuery = true)
    Optional<Agent> findByUserId(@Param("userId") Long userId);
}
