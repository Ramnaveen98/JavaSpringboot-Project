package com.autobridge_api.feedback;

import com.autobridge_api.feedback.dto.FeedbackDtos.CreateFeedbackRequest;
import com.autobridge_api.requests.RequestStatus;
import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepo;
    private final ServiceRequestRepository requestRepo;

    public FeedbackService(FeedbackRepository feedbackRepo, ServiceRequestRepository requestRepo) {
        this.feedbackRepo = feedbackRepo;
        this.requestRepo = requestRepo;
    }

    @Transactional
    public Feedback submit(Long requestId, CreateFeedbackRequest body) {
        ServiceRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (req.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback allowed only after completion");
        }

        if (feedbackRepo.findByRequestId(requestId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already exists for this request");
        }

        Feedback fb = new Feedback();
        fb.setRequest(req);
        fb.setRating(body.rating());
        fb.setComment(body.comment());

        return feedbackRepo.save(fb);
    }

    @Transactional(readOnly = true)
    public Feedback getByRequestId(Long requestId) {
        return feedbackRepo.findByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));
    }
}
