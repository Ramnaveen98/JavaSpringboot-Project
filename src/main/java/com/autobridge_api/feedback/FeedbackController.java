package com.autobridge_api.feedback;

import com.autobridge_api.feedback.dto.FeedbackDtos.CreateFeedbackRequest;
import com.autobridge_api.feedback.dto.FeedbackDtos.FeedbackDto;
import com.autobridge_api.requests.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@Tag(name = "Feedback", description = "Leave and view feedback for completed requests")
@RestController
@RequestMapping("/api/v1")
public class FeedbackController {

    private final FeedbackService service;
    private final FeedbackRepository repo;

    public FeedbackController(FeedbackService service, FeedbackRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @Operation(summary = "Create feedback for a completed request (one per request)")
    @PostMapping("/requests/{requestId}/feedback")
    public ResponseEntity<FeedbackDto> create(@PathVariable Long requestId,
                                              @Validated @RequestBody CreateFeedbackRequest body) {
        Feedback saved = service.submit(requestId, body);
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Operation(summary = "Get feedback for a request")
    @GetMapping("/requests/{requestId}/feedback")
    @Transactional(readOnly = true)
    public ResponseEntity<FeedbackDto> getByRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(toDto(service.getByRequestId(requestId)));
    }

    @Operation(summary = "List all feedback (admin view, newest first)")
    @GetMapping("/feedback")
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

    private FeedbackDto toDto(Feedback f) {
        return new FeedbackDto(
                f.getId(),
                f.getRequest().getId(),
                f.getRating(),
                f.getComment(),
                (f.getCreatedAt() != null ? DateTimeFormatter.ISO_INSTANT.format(f.getCreatedAt()) : null)
        );
    }
}
