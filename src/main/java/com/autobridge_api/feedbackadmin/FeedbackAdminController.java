package com.autobridge_api.feedbackadmin;

import com.autobridge_api.feedback.FeedbackRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback Admin", description = "Admin-only feedback actions")
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackAdminController {
    private final FeedbackRepository feedbacks;
    private final FeedbackAdminMetaRepository metas;

    public FeedbackAdminController(FeedbackRepository feedbacks, FeedbackAdminMetaRepository metas) {
        this.feedbacks = feedbacks; this.metas = metas;
    }

    @Operation(summary = "Admin: acknowledge/unacknowledge a feedback")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> acknowledge(@PathVariable Long id, @RequestBody FeedbackAcknowledgeRequest body) {
        // ensure feedback exists (do not modify your Feedback entity)
        feedbacks.findById(id).orElseThrow(() -> new IllegalArgumentException("Feedback not found: " + id));

        boolean ack = body.getAcknowledged() != null && body.getAcknowledged();
        FeedbackAdminMeta meta = metas.findByFeedbackId(id).orElse(FeedbackAdminMeta.of(id, false));
        meta.setAcknowledged(ack);
        metas.save(meta);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Admin: read acknowledged state for a feedback")
    @GetMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeedbackAdminMeta> getMeta(@PathVariable Long id) {
        FeedbackAdminMeta meta = metas.findByFeedbackId(id).orElse(FeedbackAdminMeta.of(id, false));
        return ResponseEntity.ok(meta);
    }
}
