// src/main/java/com/autobridge_api/admin/RequestAdminServiceImpl.java
package com.autobridge_api.admin;

import com.autobridge_api.agents.Agent;
import com.autobridge_api.agents.AgentRepository;
import com.autobridge_api.requests.RequestStatus;
import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.autobridge_api.admin.RequestAdminService;

import java.util.List;

@Service
public class RequestAdminServiceImpl implements RequestAdminService {

    private final ServiceRequestRepository requests;
    private final AgentRepository agents;

    public RequestAdminServiceImpl(ServiceRequestRepository requests, AgentRepository agents) {
        this.requests = requests;
        this.agents = agents;
    }

    @Override
    public List<AdminRequestsController.RequestRowDto> listAdminRows() {
        return requests.findAllAdminRows().stream()
                .map(p -> new AdminRequestsController.RequestRowDto(
                        p.getId(),
                        p.getServiceName(),
                        p.getStatus().name(),
                        p.getAssignedAgentName()
                ))
                .toList();
    }

    @Override
    public void assignAgent(long requestId, long agentId) {
        ServiceRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        Agent a = agents.findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        r.setAssignedAgent(a);

        // Put into ASSIGNED unless already in-progress or terminal
        RequestStatus st = r.getStatus();
        if (st == null
                || (st != RequestStatus.IN_PROGRESS
                && st != RequestStatus.COMPLETED
                && st != RequestStatus.CANCELLED)) {
            r.setStatus(RequestStatus.ASSIGNED);
        }

        requests.save(r);
    }

    @Override
    public void cancelRequest(long requestId, String reason) {
        ServiceRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        // No cancelReason field on your entity — just mark CANCELLED.
        r.setStatus(RequestStatus.CANCELLED);
        requests.save(r);
    }

    @Override
    public void completeRequest(long requestId) {
        ServiceRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        r.setStatus(RequestStatus.COMPLETED);
        requests.save(r);
    }

    // NOTE: This is intentionally NOT annotated with @Override
    // because your RequestAdminService interface doesn't declare it.
    // Keep it if another controller (e.g., staff actions) calls into the service.
    public void startRequest(long requestId) {
        ServiceRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        r.setStatus(RequestStatus.IN_PROGRESS);
        requests.save(r);
    }
}
