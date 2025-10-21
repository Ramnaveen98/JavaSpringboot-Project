package com.autobridge_api.requests;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dedicated controller for ADMIN CSV export.
 * Supports both:
 *  - GET /api/v1/admin/requests/export
 *  - GET /api/v1/requests/admin/export
 */
@RestController
@RequestMapping("/api/v1")
public class AdminExportController {

    private final ServiceRequestService service;

    public AdminExportController(ServiceRequestService service) {
        this.service = service;
    }

    @GetMapping(
            value = {"/admin/requests/export", "/requests/admin/export"},
            produces = "text/csv"
    )
    @Transactional(readOnly = true)
    public void exportCsv(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "America/Detroit") String tz,
            HttpServletResponse response
    ) throws IOException {

        ZoneId zone = ZoneId.of(tz);
        Instant from = (fromDate != null) ? fromDate.atStartOfDay(zone).toInstant() : null;
        Instant to   = (toDate   != null) ? toDate.plusDays(1).atStartOfDay(zone).toInstant() : null;

        // If you already have RequestSpecs.byFilters(...) keep using it:
        Specification<ServiceRequest> spec = RequestSpecs.byFilters(status, agentId, from, to);

        // Your ServiceRequestService should expose a search that returns ALL rows with a Sort.
        // If you only have a pageable version, fetch pages in a loop or add a repo.findAll(spec, sort).
        List<ServiceRequest> items = service.searchAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader("Content-Disposition", "attachment; filename=\"requests.csv\"");

        var out = response.getWriter();
        out.println(String.join(",",
                "id",
                "status",
                "createdAtLocal",
                "service",
                "slotId",
                "agentId",
                "agentName",
                "userName",
                "userEmail",
                "city",
                "state",
                "vehicleId",
                "vehicleVin"
        ));

        DateTimeFormatter localFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);

        for (ServiceRequest r : items) {
            String id           = nz(r.getId());
            String st           = r.getStatus() != null ? r.getStatus().name() : "";
            String createdLocal = (r.getCreatedAt() != null) ? localFmt.format(r.getCreatedAt()) : "";

            // ---- robust field resolution (no assumptions about exact getters) ----
            String serviceName  = resolveServiceName(r);
            String slotId       = resolveSlotId(r);
            String agentIdStr   = resolveAgentId(r);
            String agentName    = resolveAgentName(r);
            String userName     = resolveUserName(r);
            String userEmail    = resolveUserEmail(r);
            String city         = resolveCity(r);
            String state        = resolveState(r);
            String vehicleId    = resolveVehicleId(r);
            String vehicleVin   = resolveVehicleVin(r);

            out.println(String.join(",",
                    csv(id),
                    csv(st),
                    csv(createdLocal),
                    csv(serviceName),
                    csv(slotId),
                    csv(agentIdStr),
                    csv(agentName),
                    csv(userName),
                    csv(userEmail),
                    csv(city),
                    csv(state),
                    csv(vehicleId),
                    csv(vehicleVin)
            ));
        }

        out.flush();
    }

    // --------------------------- helpers ---------------------------

    private static String nz(Long v) { return v == null ? "" : v.toString(); }
    private static String nz(String s) { return s == null ? "" : s; }

    /** Try several common shapes for service name: direct field; nested offering/service. */
    private static String resolveServiceName(ServiceRequest r) {
        try {
            // Many projects store a denormalized name:
            // e.g., getServiceName()
            var m = ServiceRequest.class.getMethod("getServiceName");
            Object val = m.invoke(r);
            if (val instanceof String s && !s.isBlank()) return s;
        } catch (Exception ignored) {}

        // Try getServiceOffering().getName()
        try {
            var mOffering = ServiceRequest.class.getMethod("getServiceOffering");
            Object offering = mOffering.invoke(r);
            if (offering != null) {
                var mName = offering.getClass().getMethod("getName");
                Object val = mName.invoke(offering);
                if (val instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {}

        // Try getService().getName()
        try {
            var mService = ServiceRequest.class.getMethod("getService");
            Object svc = mService.invoke(r);
            if (svc != null) {
                var mName = svc.getClass().getMethod("getName");
                Object val = mName.invoke(svc);
                if (val instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {}

        return "";
    }

    private static String resolveSlotId(ServiceRequest r) {
        try {
            var m = ServiceRequest.class.getMethod("getSlot");
            Object slot = m.invoke(r);
            if (slot != null) {
                var mid = slot.getClass().getMethod("getId");
                Object id = mid.invoke(slot);
                if (id != null) return id.toString();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveAgentId(ServiceRequest r) {
        try {
            var m = ServiceRequest.class.getMethod("getAssignedAgent");
            Object a = m.invoke(r);
            if (a != null) {
                var mid = a.getClass().getMethod("getId");
                Object id = mid.invoke(a);
                if (id != null) return id.toString();
            }
        } catch (Exception ignored) {}
        try {
            var m = ServiceRequest.class.getMethod("getAgentId");
            Object id = m.invoke(r);
            if (id != null) return id.toString();
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveAgentName(ServiceRequest r) {
        // Try denormalized agent name/email on the request
        for (String getter : new String[]{"getAgentName", "getAgentFullName", "getAgentEmail"}) {
            try {
                var m = ServiceRequest.class.getMethod(getter);
                Object val = m.invoke(r);
                if (val instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }
        // Try assigned agent first/last/email
        try {
            var m = ServiceRequest.class.getMethod("getAssignedAgent");
            Object a = m.invoke(r);
            if (a != null) {
                String first = "";
                String last  = "";
                try {
                    var mf = a.getClass().getMethod("getFirstName");
                    Object v = mf.invoke(a);
                    if (v instanceof String s) first = s;
                } catch (Exception ignored) {}
                try {
                    var ml = a.getClass().getMethod("getLastName");
                    Object v = ml.invoke(a);
                    if (v instanceof String s) last = s;
                } catch (Exception ignored) {}
                String full = (first + " " + last).trim();
                if (!full.isBlank()) return full;
                try {
                    var me = a.getClass().getMethod("getEmail");
                    Object v = me.invoke(a);
                    if (v instanceof String s && !s.isBlank()) return s;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveUserName(ServiceRequest r) {
        // common denormalized getters
        for (String getter : new String[]{"getUserName", "getUserFullName"}) {
            try {
                var m = ServiceRequest.class.getMethod(getter);
                Object v = m.invoke(r);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }
        // else compose first + last
        String first = "";
        String last  = "";
        try {
            var m = ServiceRequest.class.getMethod("getUserFirstName");
            Object v = m.invoke(r);
            if (v instanceof String s) first = s;
        } catch (Exception ignored) {}
        try {
            var m = ServiceRequest.class.getMethod("getUserLastName");
            Object v = m.invoke(r);
            if (v instanceof String s) last = s;
        } catch (Exception ignored) {}
        String full = (first + " " + last).trim();
        return full;
    }

    private static String resolveUserEmail(ServiceRequest r) {
        for (String getter : new String[]{"getUserEmail", "getEmail"}) {
            try {
                var m = ServiceRequest.class.getMethod(getter);
                Object v = m.invoke(r);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }
        return "";
    }

    private static String resolveCity(ServiceRequest r) {
        // slot.city if available
        try {
            var m = ServiceRequest.class.getMethod("getSlot");
            Object slot = m.invoke(r);
            if (slot != null) {
                var mc = slot.getClass().getMethod("getCity");
                Object v = mc.invoke(slot);
                if (v instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {}
        // direct request city
        try {
            var m = ServiceRequest.class.getMethod("getCity");
            Object v = m.invoke(r);
            if (v instanceof String s && !s.isBlank()) return s;
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveState(ServiceRequest r) {
        try {
            var m = ServiceRequest.class.getMethod("getSlot");
            Object slot = m.invoke(r);
            if (slot != null) {
                var ms = slot.getClass().getMethod("getState");
                Object v = ms.invoke(slot);
                if (v instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {}
        try {
            var m = ServiceRequest.class.getMethod("getState");
            Object v = m.invoke(r);
            if (v instanceof String s && !s.isBlank()) return s;
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveVehicleId(ServiceRequest r) {
        // inventoryVehicle.id
        try {
            var m = ServiceRequest.class.getMethod("getInventoryVehicle");
            Object veh = m.invoke(r);
            if (veh != null) {
                var mid = veh.getClass().getMethod("getId");
                Object v = mid.invoke(veh);
                if (v != null) return v.toString();
            }
        } catch (Exception ignored) {}
        // request vehicleId
        try {
            var m = ServiceRequest.class.getMethod("getVehicleId");
            Object v = m.invoke(r);
            if (v != null) return v.toString();
        } catch (Exception ignored) {}
        return "";
    }

    private static String resolveVehicleVin(ServiceRequest r) {
        try {
            var m = ServiceRequest.class.getMethod("getInventoryVehicle");
            Object veh = m.invoke(r);
            if (veh != null) {
                var mvin = veh.getClass().getMethod("getVin");
                Object v = mvin.invoke(veh);
                if (v instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {}
        try {
            var m = ServiceRequest.class.getMethod("getVehicleVin");
            Object v = m.invoke(r);
            if (v instanceof String s && !s.isBlank()) return s;
        } catch (Exception ignored) {}
        return "";
    }

    /** Quote/escape per CSV rules. */
    private static String csv(String s) {
        if (s == null) return "";
        boolean needs = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String esc = s.replace("\"", "\"\"");
        return needs ? "\"" + esc + "\"" : esc;
    }
}
