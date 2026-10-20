
package com.example.attendance.service;

import com.example.attendance.entity.NotificationStatus;
import com.example.attendance.repository.EmailNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationClaimService {

    private final EmailNotificationRepository
            emailNotificationRepository;

    public NotificationClaimService(
            EmailNotificationRepository emailNotificationRepository
    ) {
        this.emailNotificationRepository =
                emailNotificationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimNotification(Long notificationId) {

        LocalDateTime now = LocalDateTime.now();

        int updatedRows =
                emailNotificationRepository.claimNotification(
                        notificationId,
                        NotificationStatus.PROCESSING,
                        List.of(
                                NotificationStatus.PENDING,
                                NotificationStatus.FAILED
                        ),
                        now
                );

        return updatedRows == 1;
    }
}
