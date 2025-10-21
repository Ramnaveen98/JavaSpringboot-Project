package com.autobridge_api.feedback;

import com.autobridge_api.requests.RequestStatus;
import com.autobridge_api.requests.ServiceRequest;
import com.autobridge_api.requests.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository repo;
    private final ServiceRequestRepository requests;

    public Feedback addFeedback(Long requestId, String authorEmail, Integer rating, String comment) {
        ServiceRequest req = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (req.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback allowed only after COMPLETED");
        }

        // Either style works; pick one:

        // 1) existence check:
        if (repo.existsByRequestIdAndAuthorEmail(requestId, authorEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted by this user");
        }

        // 2) or use Optional:
        // repo.findFirstByRequestIdAndAuthorEmail(requestId, authorEmail)
        //     .ifPresent(f -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback already submitted by this user"); });

        Feedback f = new Feedback();
        f.setRequest(req);
        f.setAuthorEmail(authorEmail);
        f.setRating(rating);
        f.setComment(comment);

        return repo.save(f);
    }

    public List<Feedback> listByRequest(Long requestId) {
        return repo.findByRequestId(requestId);
    }
}
