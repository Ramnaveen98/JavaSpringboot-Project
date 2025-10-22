package com.autobridge_api.agents;

import com.autobridge_api.auth.AccountRole;
import com.autobridge_api.auth.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the legacy `agents` table synchronized with the canonical `users` table
 * for users that have the AGENT role. Uses email (case-insensitive) as the join key.
 *
 * NOTE: This implementation intentionally does NOT reference a non-existent Agent.userId field.
 */
@Service
public class AgentSyncService {

    private final AgentRepository agents;

    public AgentSyncService(AgentRepository agents) {
        this.agents = agents;
    }

    /** Upsert (create or update) the legacy Agent row for a UserAccount with role=AGENT. */
    @Transactional
    public void upsertForUser(UserAccount u) {
        upsertForUser(u, null);
    }

    /**
     * Upsert (create or update) the legacy Agent row for a UserAccount with role=AGENT.
     * If the user's email changed, pass the oldEmail so we can deactivate the old agent row.
     */
    @Transactional
    public void upsertForUser(UserAccount u, String oldEmail) {
        if (u == null) return;

        // If role isn't AGENT, deactivate any legacy agent row and return.
        if (u.getRole() != AccountRole.AGENT) {
            deactivateByEmail(oldEmail != null ? oldEmail : u.getEmail());
            return;
        }

        String email = normalize(u.getEmail());
        if (email.isBlank()) return;

        // If email changed, deactivate the old row first
        if (oldEmail != null) {
            String oldNorm = normalize(oldEmail);
            if (!oldNorm.isBlank() && !oldNorm.equalsIgnoreCase(email)) {
                deactivateByEmail(oldNorm);
            }
        }

        agents.findByEmailIgnoreCase(email).ifPresentOrElse(a -> {
            // Update
            a.setFirstName(safe(u.getFirstName()));
            a.setLastName(safe(u.getLastName()));
            a.setEmail(email);
            a.setActive(true);
            agents.save(a);
        }, () -> {
            // Create
            Agent a = new Agent();
            a.setFirstName(safe(u.getFirstName()));
            a.setLastName(safe(u.getLastName()));
            a.setEmail(email);
            a.setActive(true);
            agents.save(a);
        });
    }

    /** Deactivate (soft-delete) the legacy Agent row for an email, if present. */
    @Transactional
    public void deactivateByEmail(String email) {
        String em = normalize(email);
        if (em.isBlank()) return;
        agents.findByEmailIgnoreCase(em).ifPresent(a -> {
            a.setActive(false);
            agents.save(a);
        });
    }

    /* ---------------- helpers ---------------- */

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
