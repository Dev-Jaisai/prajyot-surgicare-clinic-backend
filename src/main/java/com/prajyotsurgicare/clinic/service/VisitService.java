package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.dto.BillingRequest;
import com.prajyotsurgicare.clinic.dto.DirectCheckupRequest;
import com.prajyotsurgicare.clinic.dto.PatientRegistrationRequest;
import com.prajyotsurgicare.clinic.dto.VisitResponse;
import com.prajyotsurgicare.clinic.entity.*;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.enums.VisitType;
import com.prajyotsurgicare.clinic.mapper.VisitMapper;
import com.prajyotsurgicare.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final PrescriptionFileRepository fileRepository;
    private final VisitRepository visitRepository;
    private final VisitMapper visitMapper;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    // 📞 1. APPOINTMENT / WALK-IN REGISTRATION
    @Transactional
    public Visit createVisitWithClinic(Patient patient, PatientRegistrationRequest request, Long clinicId) {

        // 🔥 CRITICAL LOGS
        log.info("🚀 VISIT CREATION STARTED");
        log.info("📍 FROM FLUTTER -> ClinicID: {}, DoctorID in Request: {}", clinicId, request.getDoctorId());
        log.info("👤 PATIENT: {}, Mobile: {}", patient.getName(), patient.getMobile());

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new RuntimeException("Clinic not found for ID: " + clinicId));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found for ID: " + request.getDoctorId()));

        log.info("✅ ENTITIES FOUND -> Clinic: {}, Doctor: {}", clinic.getName(), doctor.getName());

        Visit visit = visitMapper.toVisit(patient, request);
        visit.setClinic(clinic);
        visit.setDoctor(doctor);
        visit.setCreatedAt(LocalDateTime.now());

        // 🔥🔥 FEE CALCULATION LOGIC (UPDATED) 🔥🔥
        Double fee;

        // ✅ जर ON_CALL असेल तर फी 0 आणि पेमेंट जमा दाखवा
        if (request.getVisitType() == VisitType.ON_CALL) {
            log.info("📞 ON_CALL Visit detected. Setting Fee to 0.0 and Auto-Collecting Payment.");
            fee = 0.0;
            visit.setPaymentCollected(true); // ✅ Payment Done
        } else {
            // ✅ रेग्युलर व्हिजिट
            fee = calculateConsultationFee(patient.getId(), doctor.getId());
            visit.setPaymentCollected(false); // ✅ Payment Pending
        }

        visit.setConsultationFee(fee);

        // ✅ FIX: 'double' असल्यामुळे null check ची गरज नाही directly value घ्या
        double otherCharges = request.getOtherCharges();

        visit.setTotalAmount(fee + otherCharges);
        // ---------------------------------------------

        // Cancel old future reminders
        notificationService.cancelFutureReminders(patient.getId());

        // Date Logic
        if (request.isAppointment() && request.getAppointmentDate() != null) {
            visit.setVisitDate(request.getAppointmentDate());
        } else {
            visit.setVisitDate(LocalDate.now());
        }

        // Token Logic
        Integer maxToken = visitRepository.findMaxTokenByDoctor(
                visit.getVisitDate(),
                clinicId,
                doctor.getId()
        );
        if (maxToken == null) maxToken = 0;
        visit.setTokenNumber(maxToken + 1);

        // Status & Queue Logic
        if (request.isAppointment()) {
            visit.setStatus(VisitStatus.BOOKED);
        } else {
            visit.setStatus(VisitStatus.ARRIVED);
            visit.setQueueOrder(LocalTime.now().toSecondOfDay());
        }

        Visit saved = visitRepository.save(visit);

        log.info("💾 VISIT SAVED SUCCESSFULLY! ID: {}, Fee: {}, Type: {}",
                saved.getId(), saved.getTotalAmount(), saved.getVisitType());

        // Transactional WebSocket
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!request.isAppointment()) {
                    if (saved.isEmergency()) {
                        webSocketService.sendQueueUpdate(clinicId, "EMERGENCY");
                    } else {
                        webSocketService.sendQueueUpdate(clinicId, "REFRESH_QUEUE");
                    }
                }
            }
        });

        // SMS (Async)
        if (request.isAppointment() && patient.getMobile() != null) {
            notificationService.sendAppointmentConfirmation(
                    patient.getName(),
                    patient.getMobile(),
                    saved.getVisitDate().toString(),
                    saved.getTokenNumber(),
                    saved.getDoctor().getName(),
                    saved.getClinic().getName()
            );
        }
        return saved;
    }
    private Double calculateConsultationFee(Long patientId, Long doctorId) {
        log.info("--------------------------------------------------");
        log.info("💰 FEE CALCULATION START for Patient ID: {}", patientId);

        // 1. फक्त पेशंट ID वापरून हिस्ट्री काढा
        List<Visit> pastVisits = visitRepository.findLastVisits(patientId);

        log.info("📜 Found {} past COMPLETED visits.", pastVisits.size());

        if (!pastVisits.isEmpty()) {
            Visit lastVisit = pastVisits.get(0);
            LocalDate lastDate = lastVisit.getVisitDate();
            long days = java.time.temporal.ChronoUnit.DAYS.between(lastDate, LocalDate.now());

            log.info("📅 Last Completed Visit: {}", lastDate);
            log.info("⏳ Days since last visit: {}", days);

            if (days >= 0 && days <= 30) {
                log.info("✅ Result: FOLLOW-UP (Fee: 300)");
                log.info("--------------------------------------------------");
                return 300.0;
            } else {
                log.info("❌ Result: EXPIRED (>30 days) (Fee: 500)");
            }
        } else {
            log.info("❌ No COMPLETED history found (New/First Time).");
        }

        log.info("💵 Final Fee Applied: 500.0");
        log.info("--------------------------------------------------");
        return 500.0;
    }

    // 📍 2. MARK ARRIVED
    @Transactional
    public void markArrived(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        if (!visit.getVisitDate().equals(LocalDate.now())) {
            log.info("🗓️ Rescheduling Visit ID {} from {} to TODAY",
                    visitId, visit.getVisitDate());
            visit.setVisitDate(LocalDate.now());
        }

        visit.setStatus(VisitStatus.ARRIVED);

        if (visit.getQueueOrder() == null) {
            visit.setQueueOrder(LocalTime.now().toSecondOfDay());
        }

        visitRepository.save(visit);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
            }
        });
    }

    // 💰 3. COLLECT PAYMENT (✅ CRITICAL FIX)
    @Transactional
    public void collectPayment(BillingRequest request) {
        Visit visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        if (request.getAmount() != null) {
            visit.setPaidAmount(request.getAmount());
        } else {
            Double total = (request.getConsultationFee() != null ? request.getConsultationFee() : 0)
                    + (request.getOtherCharges() != null ? request.getOtherCharges() : 0);
            visit.setPaidAmount(total);
        }

        visit.setProcedures(request.getProcedures());
        visit.setStatus(VisitStatus.COMPLETED);

        // ✅ CRITICAL: फक्त नवीन follow-up असेल तरच UPDATE
        if (request.getFollowUpDate() != null) {
            visit.setFollowUpDate(request.getFollowUpDate());
            log.info("✅ Follow-up set for Visit ID {}: {}", visit.getId(), request.getFollowUpDate());
        } else {
            log.info("⚠️ No follow-up date in billing request for Visit ID {}", visit.getId());
            // ❌ जुनी follow-up date delete करू नका!
        }

        visitRepository.save(visit);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
            }
        });
    }

    // 📜 4. HISTORY
    public List<VisitResponse> getVisitHistory(Long patientId) {
        return visitRepository
                .findByPatientIdOrderByVisitDateDesc(patientId)
                .stream()
                .map(v -> {
                    boolean hasFile = !fileRepository.findByVisitId(v.getId()).isEmpty();                    String docName = (v.getDoctor() != null) ? v.getDoctor().getName() : "Unknown";
                    String clinicName = (v.getClinic() != null) ? v.getClinic().getName() : "Unknown";

                    return VisitResponse.builder()
                            .visitId(v.getId())
                            .patientName(v.getPatient().getName())
                            .visitType(v.getVisitType())
                            .status(v.getStatus())
                            .isEmergency(v.isEmergency())
                            .reason(v.getReason())
                            .visitDate(v.getVisitDate())
                            .doctorName(docName)
                            .clinicName(clinicName)
                            .diagnosis(v.getDiagnosis())

                            // ✅ NEW: Prescription Note जोडा
                            .prescription(v.getPrescriptionNote())

                            .totalAmount(v.getPaidAmount())
                            .hasFile(hasFile)
                            .followUpDate(v.getFollowUpDate())
                            .build();
                })
                .toList();
    }

    @Transactional
    public Long processDirectCheckup(DirectCheckupRequest req) {
        // 1. Find Entities
        Patient patient = patientRepository.findById(req.getPatientId()).orElseThrow();
        Doctor doctor = doctorRepository.findById(req.getDoctorId()).orElseThrow();
        Clinic clinic = clinicRepository.findById(req.getClinicId()).orElseThrow();

        // 2. Create Visit (Directly COMPLETED)
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setClinic(clinic);
        visit.setVisitDate(LocalDate.now());
        visit.setStatus(VisitStatus.COMPLETED); // 🔥 Direct Complete
        visit.setVisitType(VisitType.OPD);
        visit.setCreatedAt(LocalDateTime.now());
        visit.setCompletionDateTime(LocalDateTime.now());

        // 3. Set Data
        visit.setDiagnosis(req.getDiagnosis());
        visit.setPrescriptionNote(req.getPrescription());
        visit.setBp(req.getBp());
        visit.setWeight(req.getWeight());
        visit.setTemperature(req.getTemperature());
        visit.setFollowUpDate(req.getFollowUpDate());

        // 4. Set Billing
        visit.setConsultationFee(req.getConsultationFee());
        visit.setOtherCharges(req.getOtherCharges());
        visit.setTotalAmount(req.getTotalAmount());
        visit.setPaidAmount(req.getTotalAmount());
        visit.setPaymentMode("CASH");

        // 5. Token Logic (Optional but good)
        Integer maxToken = visitRepository.findMaxTokenByDoctor(LocalDate.now(), req.getClinicId(), req.getDoctorId());
        visit.setTokenNumber((maxToken == null ? 0 : maxToken) + 1);

        visitRepository.save(visit);
        return visit.getId();
    }
}