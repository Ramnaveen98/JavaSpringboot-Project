package com.autobridge_api.admin.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminRequestRow {
    private Long id;
    private String serviceName;
    private String status;
    private String userName;
    private String userEmail;
    private Long agentId;
    private String agentName;
    private String startAtLocal; // optional
}
