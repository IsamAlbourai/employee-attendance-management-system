
package com.example.attendance.service;

import com.example.attendance.entity.EmailNotification;
import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationDeliveryService {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    NotificationDeliveryService.class
            );

    private final EmailNotificationRepository
            emailNotificationRepository;

    private final NotificationClaimService
            notificationClaimService;

    private final NotificationResultService
            notificationResultService;

    private final EmailService emailService;

    public NotificationDeliveryService(
            EmailNotificationRepository emailNotificationRepository,
            NotificationClaimService notificationClaimService,
            NotificationResultService notificationResultService,
            EmailService emailService
    ) {
        this.emailNotificationRepository =
                emailNotificationRepository;

        this.notificationClaimService =
                notificationClaimService;

        this.notificationResultService =
                notificationResultService;

        this.emailService = emailService;
    }

    public void processPendingNotifications() {

        List<EmailNotification> notifications =
                emailNotificationRepository
                        .findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                List.of(
                                        NotificationStatus.PENDING,
                                        NotificationStatus.FAILED
                                ),
                                LocalDateTime.now()
                        );

        for (EmailNotification notification : notifications) {

            Long notificationId = notification.getId();

            boolean claimed =
                    notificationClaimService
                            .claimNotification(notificationId);

            if (!claimed) {
                continue;
            }

            EmailNotification claimedNotification =
                    emailNotificationRepository
                            .findById(notificationId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Claimed notification not found"
                                    )
                            );

            try {

                emailService.sendEmail(
                        claimedNotification.getRecipientEmail(),
                        claimedNotification.getSubject(),
                        claimedNotification.getBody()
                );

            } catch (RuntimeException exception) {

                logger.warn(
                        "Email delivery failed for notification ID {}",
                        notificationId,
                        exception
                );

                notificationResultService
                        .markAsFailed(notificationId);

                continue;
            }

            notificationResultService
                    .markAsSent(notificationId);

            logger.info(
                    "Email notification {} sent successfully",
                    notificationId
            );
        }
    }
}
