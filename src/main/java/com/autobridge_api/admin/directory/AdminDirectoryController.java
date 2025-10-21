package com.autobridge_api.admin.directory;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.auth.AccountRole;
import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin directory that manages people in UserAccount and keeps the Agents table in sync.
 * - If role becomes AGENT => ensure Agent row exists (create/activate).
 * - If role becomes USER/ADMIN => deactivate Agent row (do not delete user).
 * - Listing agents returns both users.id and agents.id so UI can choose either.
 */
@RestController
@RequestMapping("/api/v1/admin/directory")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDirectoryController {

    public record PersonDto(
            Long userId,
            Long agentId,         // may be null if not an agent (or not yet synced)
            String firstName,
            String lastName,
            String email,
            String role,          // "USER" | "AGENT" | "ADMIN"
            Boolean agentActive   // null if no agent row
    ) {}

    public record UpsertDto(
            String firstName,
            String lastName,
            String email,
            String role // optional; if null, defaults applied per endpoint
    ) {}

    private final UserAccountRepository users;
    private final AgentRepository agents;

    public AdminDirectoryController(UserAccountRepository users, AgentRepository agents) {
        this.users = users;
        this.agents = agents;
    }

    /* ========= Agents ========= */

    @GetMapping("/agents")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PersonDto>> listAgents() {
        List<UserAccount> all = users.findAll();
        List<PersonDto> out = all.stream()
                .filter(u -> u.getRole() == AccountRole.AGENT)
                .map(this::toDtoWithAgentLookup)
                .toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/agents")
    @Transactional
    public ResponseEntity<PersonDto> createAgent(@RequestBody UpsertDto dto) {
        // Create or update user by email
        String email = safe(dto.email());
        UserAccount u = users.findByEmail(email).orElseGet(UserAccount::new);
        u.setEmail(email);
        if (dto.firstName() != null) u.setFirstName(dto.firstName());
        if (dto.lastName()  != null) u.setLastName(dto.lastName());
        // Force role to AGENT for this endpoint unless the payload explicitly says otherwise (rare)
        AccountRole role = dto.role() != null ? parseRole(dto.role(), AccountRole.AGENT) : AccountRole.AGENT;
        u.setRole(role);
        users.save(u);

        // Sync Agent row
        syncAgentFromUser(u);
        return ResponseEntity.ok(toDtoWithAgentLookup(u));
    }

    @PutMapping("/agents/{userId}")
    @Transactional
    public ResponseEntity<PersonDto> updateAgent(@PathVariable Long userId, @RequestBody UpsertDto dto) {
        UserAccount u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (dto.firstName() != null) u.setFirstName(dto.firstName());
        if (dto.lastName()  != null) u.setLastName(dto.lastName());
        if (dto.email()     != null) u.setEmail(dto.email());
        if (dto.role()      != null) u.setRole(parseRole(dto.role(), AccountRole.AGENT));
        users.save(u);

        syncAgentFromUser(u);
        return ResponseEntity.ok(toDtoWithAgentLookup(u));
    }

    @DeleteMapping("/agents/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteAgent(@PathVariable Long userId) {
        // We do NOT delete the user; we demote + deactivate the agent row
        UserAccount u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setRole(AccountRole.USER);
        users.save(u);

        agents.findByEmailIgnoreCase(u.getEmail()).ifPresent(a -> {
            a.setActive(false);
            agents.save(a);
        });
        return ResponseEntity.noContent().build();
    }

    /* ========= Users (end customers) ========= */

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PersonDto>> listUsers() {
        List<UserAccount> all = users.findAll();
        List<PersonDto> out = all.stream()
                .filter(u -> u.getRole() == AccountRole.USER)
                .map(this::toDtoWithAgentLookup)
                .toList();
        return ResponseEntity.ok(out);
    }

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<PersonDto> createUser(@RequestBody UpsertDto dto) {
        String email = safe(dto.email());
        UserAccount u = users.findByEmail(email).orElseGet(UserAccount::new);
        u.setEmail(email);
        if (dto.firstName() != null) u.setFirstName(dto.firstName());
        if (dto.lastName()  != null) u.setLastName(dto.lastName());
        // Default USER unless supplied
        u.setRole(dto.role() != null ? parseRole(dto.role(), AccountRole.USER) : AccountRole.USER);
        users.save(u);

        // If created as AGENT here, sync agent row too
        syncAgentFromUser(u);
        return ResponseEntity.ok(toDtoWithAgentLookup(u));
    }

    @PutMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<PersonDto> updateUser(@PathVariable Long userId, @RequestBody UpsertDto dto) {
        UserAccount u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (dto.firstName() != null) u.setFirstName(dto.firstName());
        if (dto.lastName()  != null) u.setLastName(dto.lastName());
        if (dto.email()     != null) u.setEmail(dto.email());
        if (dto.role()      != null) u.setRole(parseRole(dto.role(), AccountRole.USER));
        users.save(u);

        // If role now AGENT, ensure agent row; if demoted, deactivate agent
        syncAgentFromUser(u);
        return ResponseEntity.ok(toDtoWithAgentLookup(u));
    }

    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        // If you truly delete the user, you may also want to deactivate the agent row if existed
        UserAccount u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        users.deleteById(userId);
        agents.findByEmailIgnoreCase(u.getEmail()).ifPresent(a -> {
            a.setActive(false);
            agents.save(a);
        });
        return ResponseEntity.noContent().build();
    }

    /* ========= helpers ========= */

    private void syncAgentFromUser(UserAccount u) {
        if (u.getRole() == AccountRole.AGENT) {
            Agent a = agents.findByEmailIgnoreCase(u.getEmail()).orElse(null);
            if (a == null) {
                a = new Agent();
                a.setFirstName(safeOrNull(u.getFirstName()));
                a.setLastName(safeOrNull(u.getLastName()));
                a.setEmail(safeOrNull(u.getEmail()));
                a.setPhone(safeOrNull(u.getPhone()));
                a.setActive(true);
                agents.save(a);
            } else {
                // Update stale fields if needed and activate
                if (isBlank(a.getFirstName())) a.setFirstName(safeOrNull(u.getFirstName()));
                if (isBlank(a.getLastName()))  a.setLastName(safeOrNull(u.getLastName()));
                if (isBlank(a.getPhone()))     a.setPhone(safeOrNull(u.getPhone()));
                a.setActive(true);
                agents.save(a);
            }
        } else {
            // Non-agent roles → deactivate agent row (if exists)
            agents.findByEmailIgnoreCase(u.getEmail()).ifPresent(a -> {
                if (a.isActive()) {
                    a.setActive(false);
                    agents.save(a);
                }
            });
        }
    }

    private PersonDto toDtoWithAgentLookup(UserAccount u) {
        Agent a = (u.getEmail() == null) ? null : agents.findByEmailIgnoreCase(u.getEmail()).orElse(null);
        return new PersonDto(
                u.getId(),
                a == null ? null : a.getId(),
                safe(u.getFirstName()),
                safe(u.getLastName()),
                safe(u.getEmail()),
                roleToString(u.getRole()),
                a == null ? null : a.isActive()
        );
    }

    private static AccountRole parseRole(String s, AccountRole fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return AccountRole.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static String roleToString(AccountRole r) {
        return r == null ? "" : r.name();
    }

    private static String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
    private static String safeOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
