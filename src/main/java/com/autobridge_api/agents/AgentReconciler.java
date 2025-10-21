package com.autobridge_api.agents;

import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Utility that reconciles/synchronizes legacy `agents` rows with the canonical `users` table.
 * Uses AgentSyncService.upsertForUser(UserAccount) (no `reconcile` method).
 *
 * NOTE: This class does NOT auto-run at startup to avoid surprises.
 * You can call reconcileAll() from an admin endpoint or a command, if you want a bulk sync.
 */
@Component
public class AgentReconciler {

    private final UserAccountRepository users;
    private final AgentSyncService agentSync;

    public AgentReconciler(UserAccountRepository users, AgentSyncService agentSync) {
        this.users = users;
        this.agentSync = agentSync;
    }

    /**
     * Bulk reconcile: ensure every user with role=AGENT has a corresponding row in `agents`
     * (created or updated). Users that are not AGENT will cause any existing legacy agent row
     * with that email to be deactivated by AgentSyncService (if you call upsertForUser on them).
     */
    @Transactional
    public void reconcileAll() {
        List<UserAccount> all = users.findAll();
        for (UserAccount u : all) {
            agentSync.upsertForUser(u);
        }
    }

    /**
     * Reconcile a single user (e.g., when an account changes or is created).
     */
    @Transactional
    public void reconcileOne(UserAccount u) {
        if (u == null) return;
        agentSync.upsertForUser(u);
    }

    /**
     * Reconcile a single user when the email has changed. This will deactivate the old agent row.
     */
    @Transactional
    public void reconcileOne(UserAccount u, String oldEmail) {
        if (u == null) return;
        agentSync.upsertForUser(u, oldEmail);
    }
}
