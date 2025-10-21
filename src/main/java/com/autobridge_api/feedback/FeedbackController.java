package com.autobridge_api.feedback;

import com.autobridge_api.feedback.dto.FeedbackDtos.CreateFeedbackRequest;
import com.autobridge_api.feedback.dto.FeedbackDtos.FeedbackDto;
import com.autobridge_api.requests.RequestStatus;
import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestRepository;
import com.autobridge_api.requests.dto.PageResponse;
import com.autobridge_api.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Feedback", description = "Leave and view feedback for completed requests")
@RestController
@RequestMapping("/api/v1")
public class FeedbackController {

    private final FeedbackRepository repo;
    private final ServiceRequestRepository requests;
    private final JwtService jwt;

    public FeedbackController(FeedbackRepository repo,
                              ServiceRequestRepository requests,
                              JwtService jwt) {
        this.repo = repo;
        this.requests = requests;
        this.jwt = jwt;
    }

    @Operation(summary = "Create feedback for a completed request (one per request)")
    @PostMapping("/requests/{requestId}/feedback")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackDto> create(@PathVariable Long requestId,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                              @Validated @RequestBody CreateFeedbackRequest body) {
        final String token = auth != null ? auth.replace("Bearer ", "") : "";
        final String authorEmail = (token.isBlank() ? null : jwt.extractEmail(token));

        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required.");
        }
        final int rating = body.rating();
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5.");
        }
        if (body.comment() == null || body.comment().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required.");
        }

        ServiceRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found."));

        if (req.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is allowed only after completion.");
        }

        try {
            Feedback f = Feedback.builder()
                    .request(req)
                    .rating(rating)
                    .comment(body.comment().trim())
                    .authorEmail(authorEmail)
                    .build();

            Feedback saved = repo.save(f);

            return ResponseEntity
                    .created(URI.create("/api/v1/feedback/" + saved.getId()))
                    .body(toDto(saved));

        } catch (DataIntegrityViolationException ex) {
            // Requires a DB unique constraint on feedback.request_id to enforce one-per-request
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted for this request.");
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to submit feedback right now.");
        }
    }

    @Operation(summary = "Get feedback for a request (returns all feedback entries)")
    @GetMapping("/requests/{requestId}/feedback")
    @Transactional(readOnly = true)
    public ResponseEntity<List<FeedbackDto>> listByRequest(@PathVariable Long requestId) {
        List<FeedbackDto> list = repo.findByRequestId(requestId)
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "List all feedback (admin view, newest first)")
    @GetMapping("/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<FeedbackDto>> list(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> pageData = repo.findAll(pageable);

        var items = pageData.map(this::toDto).getContent();

        PageResponse<FeedbackDto> body = new PageResponse<>(
                items,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isFirst(),
                pageData.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Agent: list feedback for my assigned requests (newest first)")
    @GetMapping("/agent/feedback")
    @PreAuthorize("hasRole('AGENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<FeedbackDto>> listForAgent(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        final String token = auth != null ? auth.replace("Bearer ", "") : "";
        final String agentEmail = (token.isBlank() ? null : jwt.extractEmail(token));
        if (agentEmail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing agent identity.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Feedback> data = repo.findByRequest_AssignedAgent_EmailOrderByCreatedAtDesc(agentEmail, pageable);

        var items = data.map(this::toDto).getContent();
        PageResponse<FeedbackDto> body = new PageResponse<>(
                items,
                data.getNumber(),
                data.getSize(),
                data.getTotalElements(),
                data.getTotalPages(),
                data.isFirst(),
                data.isLast()
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Admin: acknowledge a feedback (mark as reviewed)")
    @PatchMapping("/feedback/{id}/acknowledge")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<FeedbackDto> acknowledge(@PathVariable Long id,
                                                   @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        Feedback f = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found."));

        if (!f.isAcknowledged()) {
            final String token = auth != null ? auth.replace("Bearer ", "") : "";
            final String adminEmail = (token.isBlank() ? null : jwt.extractEmail(token));
            f.setAcknowledged(true);
            f.setAcknowledgedBy(adminEmail);
            f.setAcknowledgedAt(Instant.now());
            repo.save(f);
        }
        return ResponseEntity.ok(toDto(f));
    }

    // ---- Mapper ----
    private FeedbackDto toDto(Feedback f) {
        return new FeedbackDto(
                f.getId(),
                f.getRequest().getId(),
                f.getRating(),
                f.getComment(),
                (f.getCreatedAt() != null ? DateTimeFormatter.ISO_INSTANT.format(f.getCreatedAt()) : null),
                f.isAcknowledged(),
                f.getAcknowledgedBy(),
                (f.getAcknowledgedAt() != null ? DateTimeFormatter.ISO_INSTANT.format(f.getAcknowledgedAt()) : null)
        );
    }
}
