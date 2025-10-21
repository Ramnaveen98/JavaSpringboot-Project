package com.autobridge_api.feedbackadmin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackAdminMetaRepository extends JpaRepository<FeedbackAdminMeta, Long> {
    Optional<FeedbackAdminMeta> findByFeedbackId(Long feedbackId);
}
