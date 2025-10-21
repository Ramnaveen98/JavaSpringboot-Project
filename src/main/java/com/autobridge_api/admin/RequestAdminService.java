package com.autobridge_api.admin;

import java.util.List;

/**
 * Top-level port used by AdminRequestsController and implemented by your RequestAdminServiceImpl.
 *
 * Keep using the same row DTO the controller exposes to avoid changing your frontend.
 */
public interface RequestAdminService {
    List<AdminRequestsController.RequestRowDto> listAdminRows();
    void assignAgent(long requestId, long agentId);
    void cancelRequest(long requestId, String reason);
    void completeRequest(long requestId);
}
