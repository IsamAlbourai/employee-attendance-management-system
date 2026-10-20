
package com.example.attendance.service;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailNotificationService {

    private final EmailNotificationRepository
            emailNotificationRepository;

    public EmailNotificationService(
            EmailNotificationRepository emailNotificationRepository
    ) {
        this.emailNotificationRepository =
                emailNotificationRepository;
    }

    @Transactional
    public void queueEmail(
            String eventKey,
            String recipientEmail,
            String subject,
            String body
    ) {

        if (eventKey == null
                || eventKey.isBlank()
                || eventKey.length() > 200) {

            throw new IllegalArgumentException(
                    "Valid notification event key is required"
            );
        }

        if (recipientEmail == null
                || recipientEmail.isBlank()
                || recipientEmail.length() > 255) {

            throw new IllegalArgumentException(
                    "Valid recipient email is required"
            );
        }

        if (subject == null
                || subject.isBlank()
                || subject.length() > 255) {

            throw new IllegalArgumentException(
                    "Valid email subject is required"
            );
        }

        if (body == null
                || body.isBlank()
                || body.length() > 4000) {

            throw new IllegalArgumentException(
                    "Valid email body is required"
            );
        }

        if (emailNotificationRepository.existsByEventKey(
                eventKey
        )) {
            return;
        }

        EmailNotification notification =
                new EmailNotification();

        notification.setEventKey(eventKey);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setStatus(
                NotificationStatus.PENDING
        );
        notification.setAttemptCount(0);

        emailNotificationRepository.save(notification);
    }
}
