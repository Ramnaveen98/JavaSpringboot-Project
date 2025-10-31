package com.autobridge_api.feedback;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class FeedbackMailService {

    private final JavaMailSender mailSender;

    public FeedbackMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendThankYouEmail(String toEmail, String userName, String serviceName, String feedbackText) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("Thank you for using AutoBridge Services");
            msg.setText("""
                    Hello %s,

                    Thank you for using AutoBridge! 
                    Below are your service and feedback details:

                    Service: %s
                    Your Feedback: %s

                    We appreciate your time and support.
                    Regards,
                    AutoBridge Admin
                    """.formatted(userName != null ? userName : "User",
                    serviceName != null ? serviceName : "Your recent service",
                    feedbackText != null ? feedbackText : "(no feedback text)"));

            mailSender.send(msg);
            System.out.println("Thank-you email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send feedback thank-you email: " + e.getMessage());
        }
    }
}
