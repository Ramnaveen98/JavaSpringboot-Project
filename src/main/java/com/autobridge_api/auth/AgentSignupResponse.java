package com.autobridge_api.auth;

public class AgentSignupResponse {
    private Long userId;
    private Long agentId;
    private boolean active;

    public AgentSignupResponse() {}
    public AgentSignupResponse(Long userId, Long agentId, boolean active) {
        this.userId = userId; this.agentId = agentId; this.active = active;
    }

    public Long getUserId() { return userId; }
    public Long getAgentId() { return agentId; }
    public boolean isActive() { return active; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public void setActive(boolean active) { this.active = active; }
}
