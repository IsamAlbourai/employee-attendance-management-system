
package com.example.attendance.service;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.exception.ConflictException;
import com.example.attendance.exception.ResourceNotFoundException;
import com.example.attendance.repository.EmailNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationResultService {

    private static final int MAX_ATTEMPTS = 5;

    private final EmailNotificationRepository
            emailNotificationRepository;

    public NotificationResultService(
            EmailNotificationRepository emailNotificationRepository
    ) {
        this.emailNotificationRepository =
                emailNotificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSent(Long notificationId) {

        EmailNotification notification =
                getProcessingNotification(notificationId);

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification.setNextAttemptAt(null);
        notification.setLastError(null);

        emailNotificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long notificationId) {

        EmailNotification notification =
                getProcessingNotification(notificationId);

        notification.setStatus(NotificationStatus.FAILED);
        notification.setLastError(
                "Email delivery failed. Check application logs."
        );

        if (notification.getAttemptCount() >= MAX_ATTEMPTS) {

            notification.setNextAttemptAt(null);

        } else {

            long delayMinutes =
                    (long) Math.pow(
                            2,
                            notification.getAttemptCount() - 1
                    );

            notification.setNextAttemptAt(
                    LocalDateTime.now().plusMinutes(delayMinutes)
            );
        }

        emailNotificationRepository.save(notification);
    }

    private EmailNotification getProcessingNotification(
            Long notificationId
    ) {

        EmailNotification notification =
                emailNotificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Email notification not found"
                                )
                        );

        if (notification.getStatus()
                != NotificationStatus.PROCESSING) {

            throw new ConflictException(
                    "Notification is not currently being processed"
            );
        }

        return notification;
    }
}
