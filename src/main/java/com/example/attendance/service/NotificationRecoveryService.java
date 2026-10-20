
package com.example.attendance.service;

import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationRecoveryService {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationRecoveryService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final int PROCESSING_TIMEOUT_MINUTES = 5;

    private final EmailNotificationRepository emailNotificationRepository;

    public NotificationRecoveryService(
            EmailNotificationRepository emailNotificationRepository
    ) {
        this.emailNotificationRepository = emailNotificationRepository;
    }

    @Transactional
    public int recoverStuckNotifications() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime cutoff =
                now.minusMinutes(PROCESSING_TIMEOUT_MINUTES);

        int retryable = emailNotificationRepository
                .recoverRetryableNotifications(
                        NotificationStatus.PROCESSING,
                        NotificationStatus.FAILED,
                        cutoff,
                        now,
                        MAX_ATTEMPTS,
                        "Processing interrupted. Notification scheduled for retry."
                );

        int exhausted = emailNotificationRepository
                .recoverExhaustedNotifications(
                        NotificationStatus.PROCESSING,
                        NotificationStatus.FAILED,
                        cutoff,
                        MAX_ATTEMPTS,
                        "Processing interrupted. Maximum delivery attempts reached."
                );

        int totalRecovered = retryable + exhausted;

        if (totalRecovered > 0) {
            logger.warn(
                    "Recovered {} stuck email notifications: {} retryable, {} exhausted",
                    totalRecovered,
                    retryable,
                    exhausted
            );
        }

        return totalRecovered;
    }
}
