package com.prajyotsurgicare.clinic.scheduler;

import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import com.prajyotsurgicare.clinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final VisitRepository visitRepository;
    private final NotificationService notificationService;

    // ⏰ Cron Job: रोज सकाळी १०:०० वाजता रन होईल
    // Format: sec min hour day month year
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendDailyReminders() {

        log.info("⏰ Scheduler Started: Checking for follow-ups...");

        LocalDate today = LocalDate.now();
        List<Visit> visits = visitRepository.findByFollowUpDate(today);

        if (visits.isEmpty()) {
            log.info("✅ No follow-ups found for today.");
            return;
        }

        for (Visit v : visits) {
            if (v.getPatient().getMobile() != null) {

                // SMS पाठवा
                notificationService.sendFollowUpReminder(
                        v.getPatient().getName(),
                        v.getPatient().getMobile(),
                        v.getDoctor().getName()
                );

                log.info("📩 Reminder sent to: {}", v.getPatient().getName());
            }
        }
    }
}