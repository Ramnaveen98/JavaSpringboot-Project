package com.autobridge_api.auth;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentSignupService {
    private final UserAccountRepository users;
    private final AgentRepository agents;
    private final PasswordEncoder encoder;

    public AgentSignupService(UserAccountRepository users, AgentRepository agents, PasswordEncoder encoder) {
        this.users = users; this.agents = agents; this.encoder = encoder;
    }

    @Transactional
    public AgentSignupResponse signup(AgentSignupRequest req) {
        if (req.getEmail() == null || req.getPassword() == null)
            throw new IllegalArgumentException("email and password required");

        // Check duplicate email (without assuming repo method)
        boolean emailExists = users.findAll().stream()
                .anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(req.getEmail()));
        if (emailExists) throw new IllegalArgumentException("Email already registered");

        // Create inactive AGENT user (using setters — avoids Lombok builder dependency)
        UserAccount u = new UserAccount();
        u.setEmail(req.getEmail());
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setRole(AccountRole.AGENT);  // uses your existing enum
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setPhone(req.getPhone());
        u.setActive(false);
        u = users.save(u);

        // Create inactive Agent record (mirror basic fields)
        Agent a = new Agent();
        a.setFirstName(u.getFirstName());
        a.setLastName(u.getLastName());
        a.setEmail(u.getEmail());
        a.setPhone(u.getPhone());
        a.setActive(false);
        a = agents.save(a);

        return new AgentSignupResponse(u.getId(), a.getId(), false);
    }
}
