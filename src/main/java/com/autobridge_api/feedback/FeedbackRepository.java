package com.autobridge_api.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** Per-request list (Spring Data resolves RequestId -> request.id) */
    List<Feedback> findByRequestId(Long requestId);

    /** Admin list (newest first) */
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Agent view: ServiceRequest has `assignedAgent` (ManyToOne) and Agent has `email` */
    Page<Feedback> findByRequest_AssignedAgent_EmailOrderByCreatedAtDesc(String email, Pageable pageable);

    /** ---- Existence checks (support both naming styles) ---- */

    // Style A (used by your FeedbackService right now)
    boolean existsByRequestIdAndAuthorEmail(Long requestId, String authorEmail);

    // Style B (explicit nested property style; also valid)
    boolean existsByRequest_IdAndAuthorEmail(Long requestId, String authorEmail);

    // Optional: if you just want "one feedback per request" regardless of author
    boolean existsByRequestId(Long requestId);
    boolean existsByRequest_Id(Long requestId);
}
