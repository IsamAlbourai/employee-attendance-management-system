
package com.example.attendance.config;

import com.example.attendance.service.NotificationDeliveryService;
import com.example.attendance.service.NotificationRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class NotificationScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationDeliveryService notificationDeliveryService;

    private final NotificationRecoveryService notificationRecoveryService;

    public NotificationScheduler(
            NotificationDeliveryService notificationDeliveryService,
            NotificationRecoveryService notificationRecoveryService
    ) {
        this.notificationDeliveryService = notificationDeliveryService;
        this.notificationRecoveryService = notificationRecoveryService;
    }

    @Scheduled(
            initialDelay = 10000,
            fixedDelay = 30000
    )
    public void processEmailNotifications() {

        try {
            notificationRecoveryService.recoverStuckNotifications();

        } catch (Exception exception) {
            logger.error(
                    "Error recovering interrupted email notifications",
                    exception
            );
        }

        try {
            notificationDeliveryService.processPendingNotifications();

        } catch (Exception exception) {
            logger.error(
                    "Error processing pending email notifications",
                    exception
            );
        }
    }
}
