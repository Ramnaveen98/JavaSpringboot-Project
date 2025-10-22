package com.autobridge_api.admin;

import com.autobridge_api.auth.AccountRole;
import com.autobridge_api.auth.UserAccount;
import com.autobridge_api.auth.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserAccountRepository users;

    public AdminUsersController(UserAccountRepository users) {
        this.users = users;
    }

    public record UserDto(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone
    ) {
        static UserDto from(UserAccount ua) {
            return new UserDto(
                    ua.getId(),
                    s(ua.getFirstName()),
                    s(ua.getLastName()),
                    s(ua.getEmail()),
                    s(ua.getPhone())
            );
        }
    }

    public static class UpsertUserReq {
        @NotBlank public String firstName;
        @NotBlank public String lastName;
        @Email @NotBlank public String email;
        public String phone;
        public String password; // optional; see TODO
    }

    private static String s(Object v) { return v == null ? "" : String.valueOf(v); }

    @GetMapping
    public List<UserDto> listUsers() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == AccountRole.USER) // adjust if your Role enum differs
                .map(UserDto::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UpsertUserReq req) {
        if (users.findAll().stream().anyMatch(u -> req.email.equalsIgnoreCase(s(u.getEmail())))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        UserAccount ua = new UserAccount();
        ua.setFirstName(req.firstName);
        ua.setLastName(req.lastName);
        ua.setEmail(req.email);
        ua.setPhone(req.phone);
        ua.setRole(AccountRole.USER);
        // TODO: set password if needed via your encoder/service
        return UserDto.from(users.save(ua));
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpsertUserReq req) {
        UserAccount ua = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (ua.getRole() != AccountRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not a regular user");
        }

        boolean emailTaken = users.findAll().stream()
                .anyMatch(u -> !Objects.equals(u.getId(), id) && req.email.equalsIgnoreCase(s(u.getEmail())));
        if (emailTaken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        ua.setFirstName(req.firstName);
        ua.setLastName(req.lastName);
        ua.setEmail(req.email);
        ua.setPhone(req.phone);
        // TODO: reset password here if you want

        return UserDto.from(users.save(ua));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        UserAccount ua = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (ua.getRole() != AccountRole.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not a regular user");
        }
        users.deleteById(id);
    }
}
