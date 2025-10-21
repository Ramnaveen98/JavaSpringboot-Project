package com.autobridge_api.profile;

import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserAccountRepository users;
    public ProfileService(UserAccountRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public ProfileDto getMine() {
        UserAccount u = getCurrentUser();
        return toDto(u);
    }

    @Transactional
    public ProfileDto updateMine(UpdateProfileRequest req) {
        UserAccount u = getCurrentUser();
        if (req.getFirstName() != null) u.setFirstName(req.getFirstName());
        if (req.getLastName()  != null) u.setLastName(req.getLastName());
        if (req.getPhone()     != null) u.setPhone(req.getPhone());
        users.save(u);
        return toDto(u);
    }

    private UserAccount getCurrentUser() {
        String email = currentEmail();
        if (email == null) throw new IllegalStateException("Not authenticated");

        // Avoid repo method assumptions: use findAll() and match email (case-insensitive)
        return users.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    private static String currentEmail() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.isAuthenticated()) ? a.getName() : null; // your Jwt filter sets email as principal
    }

    private static ProfileDto toDto(UserAccount u) {
        return new ProfileDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone());
    }
}
