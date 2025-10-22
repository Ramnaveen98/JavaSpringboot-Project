package com.autobridge_api.admin.dto;

public class AdminRequestRowDto {
    private Long id;
    private String serviceName;
    private String status;
    private String assigned;

    public AdminRequestRowDto(Long id, String serviceName, Object status, String assigned) {
        this.id = id;
        this.serviceName = serviceName;
        // status might be an enum or string; normalize to string
        this.status = (status == null) ? null : status.toString();
        this.assigned = assigned;
    }

    public Long getId() { return id; }
    public String getServiceName() { return serviceName; }
    public String getStatus() { return status; }
    public String getAssigned() { return assigned; }
}
