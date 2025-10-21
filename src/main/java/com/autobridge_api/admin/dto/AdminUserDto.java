package com.autobridge_api.admin.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminUserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean active;
    private String role; // USER | AGENT | ADMIN
}
