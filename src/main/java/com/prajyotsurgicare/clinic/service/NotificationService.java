package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor // ✅ VisitRepository इंजेक्ट करण्यासाठी हे हवे
public class NotificationService {

    private final VisitRepository visitRepository; // ✅ DB ॲक्सेस ॲड करा

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
            log.info("✅ Twilio Initialized Successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize Twilio: {}", e.getMessage());
        }
    }

    // 📩 1. Appointment Confirmed SMS
    @Async
    public void sendAppointmentConfirmation(String patientName, String mobile, String date, int token, String doctorName, String clinicName) {
        String formattedMobile = formatMobileNumber(mobile);
        String text = String.format(
                "🏥 *Prajyot Surgicare*\nHello %s,\nAppt Confirmed!\n📅 Date: %s\n👨‍⚕️ Dr: %s\n🏥 Loc: %s\n🔢 Token: %d\nPls arrive 10 mins early.",
                patientName, date, doctorName, clinicName, token
        );
        sendSms(formattedMobile, text);
    }

    @Async
    public void sendWalkInConfirmation(String patientName, String mobile, int token, String doctorName, String clinicName) {
        String formattedMobile = formatMobileNumber(mobile);
        String text = String.format(
                "🏥 *Prajyot Surgicare*\nHello %s,\nRegistration Successful!\n👨‍⚕️ Dr: %s\n🏥 Loc: %s\n🔢 Your Token: %d\nPlease wait for your turn.",
                patientName, doctorName, clinicName, token
        );
        sendSms(formattedMobile, text);
    }


    // 📩 2. Thank You SMS
    @Async
    public void sendThankYouMessage(String patientName, String mobile) {
        String formattedMobile = formatMobileNumber(mobile);
        String text = String.format(
                "🏥 *Prajyot Surgicare Clinic*\n\nDear %s,\nThank you for visiting us. Get well soon!\n\nFor queries call: 9284265655",
                patientName
        );
        sendSms(formattedMobile, text);
    }

    // ✅ 3. NEW: CANCEL FUTURE REMINDERS (Updated)
    public void cancelFutureReminders(Long patientId) {
        try {
            // पेशंटच्या सर्व व्हिजिट्स आणा
            List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patientId);

            for (Visit v : visits) {
                // लॉजिक: जर भविष्यातील तारीख असेल तर...
                if (v.getFollowUpDate() != null && v.getFollowUpDate().isAfter(LocalDate.now())) {

                    // ❌ हे दोन लाईन्स कमेंट करा! (Disable Delete)
                    // log.info("🚫 Auto-Cancelling previous follow-up for Patient ID: {} Date: {}", patientId, v.getFollowUpDate());
                    // v.setFollowUpDate(null);
                    // visitRepository.save(v);

                    log.info("ℹ️ Found existing future follow-up: {}. Keeping it active.", v.getFollowUpDate());
                }
            }
        } catch (Exception e) {
            log.error("⚠️ Failed to cancel future reminders for Patient ID: {}", patientId, e);
        }
    }

    // 📩 4. FOLLOW-UP REMINDER SMS (Cron Job साठी)
    @Async
    public void sendFollowUpReminder(String patientName, String mobile, String doctorName) {
        String formattedMobile = formatMobileNumber(mobile);
        String text = String.format(
                "🏥 *Reminder from Prajyot Surgicare*\n\nHello %s,\nYour follow-up visit with %s is scheduled for today/tomorrow.\nPlease visit the clinic for a checkup.\n\nCall: 9284265655",
                patientName, doctorName
        );
        sendSms(formattedMobile, text);
    }

    // 🚀 MAIN SMS SENDING METHOD
    private void sendSms(String toMobile, String messageBody) {
        try {
            Message.creator(
                    new PhoneNumber(toMobile),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();
            log.info("✅ SMS Sent to {}", toMobile);
        } catch (Exception e) {
            log.error("❌ Error sending SMS to {}: {}", toMobile, e.getMessage());
        }
    }

    private String formatMobileNumber(String mobile) {
        if (!mobile.startsWith("+")) {
            return "+91" + mobile;
        }
        return mobile;
    }
}