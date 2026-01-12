package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.dto.BillingRequest;
import com.prajyotsurgicare.clinic.dto.MedicalInfoRequest;
import com.prajyotsurgicare.clinic.dto.VitalsRequest;
import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.repository.PrescriptionFileRepository;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PrescriptionFileRepository fileRepository;
    private final VisitRepository visitRepository;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;

    // ... (Other methods: getWaitingCount, getTodayQueue, etc. keep as is) ...
    public long getWaitingCount(Long clinicId, LocalDate date) {
        return visitRepository.countByStatusAndVisitDateAndClinicId(
                VisitStatus.ARRIVED,
                date != null ? date : LocalDate.now(),
                clinicId
        );
    }
    // ✅ रिसेप्शनिस्टसाठी ओव्हरलोड मेथड (Missing Method)
    // ही मेथड वेब ॲप (Angular) साठी आवश्यक आहे जिथे doctorId पाठवला जात नाही.
    public List<Map<String, Object>> getTodayQueue(Long clinicId, LocalDate date) {
        log.info("🏢 Request from Receptionist - Fetching all doctor visits for Clinic ID: {}", clinicId);
        // अंतर्गत ३ पॅरामीटरच्या मेथडला 'null' doctorId देऊन कॉल करतो
        return getTodayQueue(clinicId, date, null);
    }
    public List<Map<String, Object>> getBookedAppointments(Long clinicId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Visit> visits = visitRepository.findByVisitDateAndClinicIdAndStatusOrderByTokenNumberAsc(
                queryDate, clinicId, VisitStatus.BOOKED
        );
        return mapVisits(visits);
    }

    public List<Map<String, Object>> getCompletedVisits(Long clinicId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Visit> visits = visitRepository.findByVisitDateAndClinicIdAndStatusOrderByTokenNumberAsc(
                queryDate, clinicId, VisitStatus.COMPLETED
        );
        return mapVisits(visits);
    }

    private List<Map<String, Object>> mapVisits(List<Visit> visits) {
        return visits.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tokenNumber", v.getTokenNumber());
            map.put("visitId", v.getId());
            map.put("patientName", v.getPatient().getName());
            map.put("mobile", v.getPatient().getMobile());
            map.put("visitType", v.getVisitType());
            map.put("status", v.getStatus());
            map.put("isEmergency", v.isEmergency());
            if (v.getStatus() == VisitStatus.COMPLETED && v.getCompletionDateTime() != null) {
                map.put("time", v.getCompletionDateTime());
            } else {
                map.put("time", "Anytime");
            }
            boolean fileExists = !fileRepository.findByVisitId(v.getId()).isEmpty();            map.put("hasFile", fileExists);
            map.put("doctorName", v.getDoctor().getName());
            map.put("bp", v.getBp());
            return map;
        }).toList();
    }

    @Transactional
    public void markArrived(Long visitId) {
        Visit visit = visitRepository.findById(visitId).orElseThrow(() -> new RuntimeException("Visit not found"));
        if (!visit.getVisitDate().equals(LocalDate.now())) {
            visit.setVisitDate(LocalDate.now());
        }
        visit.setStatus(VisitStatus.ARRIVED);
        visit.setQueueOrder(LocalTime.now().toSecondOfDay());
        visitRepository.save(visit);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
            }
        });
    }

    // ✅ 6. Mark Visit as Completed (FIXED LOGIC)
    @Transactional
    public void markVisitAsCompleted(Long visitId, BillingRequest request) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();

        log.info("💰 Completing Visit ID: {}. Amount: {}", visitId, request.getAmount());

        // 1. Update Basic Fields
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setConsultationFee(request.getConsultationFee());
        visit.setOtherCharges(request.getOtherCharges());
        visit.setPaymentMode(request.getPaymentMode());
        visit.setCompletionDateTime(LocalDateTime.now());

        if(request.getAmount() != null) {
            visit.setTotalAmount(request.getAmount());
        } else {
            visit.setTotalAmount(request.getConsultationFee() + request.getOtherCharges());
        }

        // 2. Update Procedures
        if (request.getProcedures() != null && !request.getProcedures().isEmpty()) {
            String currentDiagnosis = visit.getDiagnosis();
            String baseDiagnosis = "";
            if (currentDiagnosis != null && currentDiagnosis.contains("| Proc:")) {
                baseDiagnosis = currentDiagnosis.split("\\| Proc:")[0].trim();
            } else {
                baseDiagnosis = currentDiagnosis != null ? currentDiagnosis : "";
            }
            visit.setDiagnosis(baseDiagnosis + " | Proc: " + request.getProcedures());
        }

        // 3. Update Follow-up Date (Object Level Update)
        if (request.getFollowUpDate() != null) {
            log.info("📅 Billing: Saving Follow-up Date for Visit {}: {}", visitId, request.getFollowUpDate());
            visit.setFollowUpDate(request.getFollowUpDate());
        } else {
            // Optional: If you want to keep the old date if new is null, do nothing.
            // But usually, frontend sends what is visible.
            log.warn("⚠️ No Follow-up date received in billing request for Visit ID: {}", visitId);
        }

        // 4. SAVE AND FLUSH (Immediate Commit)
        visitRepository.saveAndFlush(visit);

        // 5. SAFETY NET: Direct SQL Update (To be 100% sure)
        if (request.getFollowUpDate() != null) {
            visitRepository.updateFollowUpDateDirectly(visitId, request.getFollowUpDate());
            log.info("🔒 Double Secured: Follow-up date updated via Query.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
                if (visit.getPatient().getMobile() != null) {
                    notificationService.sendThankYouMessage(
                            visit.getPatient().getName(),
                           visit.getPatient().getMobile()
                    );
                }
            }
        });
    }

    @Transactional
    public void updateQueueOrder(List<Long> visitIds) {
        Long clinicId = null;
        for (int i = 0; i < visitIds.size(); i++) {
            Visit visit = visitRepository.findById(visitIds.get(i)).orElseThrow();
            visit.setQueueOrder(i + 1);
            visitRepository.save(visit);
            if (clinicId == null) clinicId = visit.getClinic().getId();
        }
        if (clinicId != null) {
            Long finalClinicId = clinicId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webSocketService.sendQueueUpdate(finalClinicId, "REFRESH_QUEUE");
                }
            });
        }
    }

    @Transactional
    public void updateVitals(Long visitId, VitalsRequest request) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        visit.setBp(request.getBp());
        visit.setTemperature(request.getTemperature());
        visit.setPulse(request.getPulse());
        visit.setWeight(request.getWeight());
        visitRepository.save(visit);
        webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
    }

    @Transactional
    public void markEmergency(Long visitId) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        visit.setEmergency(true);
        visit.setStatus(VisitStatus.ARRIVED);
        visitRepository.save(visit);
        webSocketService.sendQueueUpdate(visit.getClinic().getId(), "EMERGENCY");
    }
    // ✅ Updated getVisitDetails method in DashboardService.java
    public Map<String, Object> getVisitDetails(Long visitId) {
        Visit visit = visitRepository.findById(visitId).orElseThrow(() -> new RuntimeException("Visit not found"));
        Map<String, Object> map = new HashMap<>();

        // --- 🟢 EXISTING DATA ---
        map.put("visitId", visit.getId());
        map.put("patientName", visit.getPatient().getName());
        map.put("tokenNumber", visit.getTokenNumber());
        map.put("visitType", visit.getVisitType());

        // --- 🔥 MISSING DATA ADDED HERE ---
        map.put("diagnosis", visit.getDiagnosis());
        map.put("prescription", visit.getPrescriptionNote()); // Medicine Note
        map.put("doctorSetFollowUp", visit.getFollowUpDate()); // Follow-up Date

        // 🩺 Vitals
        map.put("bp", visit.getBp());
        map.put("weight", visit.getWeight());
        map.put("temperature", visit.getTemperature());
        map.put("pulse", visit.getPulse());

        // 💰 Billing Calculation
        // जर Total Amount DB मध्ये असेल तर तीच पाठवा, नाहीतर बेरीज करा
        if (visit.getTotalAmount() != null && visit.getTotalAmount() > 0) {
            map.put("totalAmount", visit.getTotalAmount());
        } else {
            Double fee = visit.getConsultationFee() != null ? visit.getConsultationFee() : 0.0;
            Double other = visit.getOtherCharges() != null ? visit.getOtherCharges() : 0.0;
            map.put("totalAmount", fee + other);
        }

        // Other Fees
        map.put("consultationFee", visit.getConsultationFee());
        map.put("otherCharges", visit.getOtherCharges());

        // History Info (Optional)
        Map<String, Object> feeInfo = calculateFee(visit.getPatient().getId(), visit.getId());
        map.put("lastVisitDate", feeInfo.get("lastVisitDate"));

        return map;
    }
    private Map<String, Object> calculateFee(Long patientId, Long currentVisitId) {
        Map<String, Object> result = new HashMap<>();
        Visit currentVisit = visitRepository.findById(currentVisitId).orElseThrow();
        Long currentDoctorId = currentVisit.getDoctor().getId();
        List<Visit> allVisits = visitRepository.findByPatientIdOrderByVisitDateDesc(patientId);
        Optional<Visit> lastVisit = allVisits.stream()
                .filter(v -> !v.getId().equals(currentVisitId))
                .filter(v -> v.getStatus() == VisitStatus.COMPLETED)
                .filter(v -> v.getDoctor().getId().equals(currentDoctorId))
                .findFirst();

        if (lastVisit.isPresent()) {
            LocalDate lastDate = lastVisit.get().getVisitDate();
            long days = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
            result.put("lastVisitDate", lastDate.toString());
            if (days >= 0 && days <= 30) {
                result.put("fee", 300.0);
                result.put("visitType", "Follow-up (" + days + " days ago)");
            } else {
                result.put("fee", 500.0);
                result.put("visitType", "Fresh Case (Expired: " + days + " days ago)");
            }
        } else {
            result.put("fee", 500.0);
            result.put("visitType", "New Case (First time for this Dr)");
            result.put("lastVisitDate", "Never");
        }
        return result;
    }

    public void uploadPrescription(Long visitId, MultipartFile file) {}

    @Transactional
    public void revertToBooked(Long visitId) {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        if (visit.getStatus() == VisitStatus.ARRIVED) {
            visit.setStatus(VisitStatus.BOOKED);
            visitRepository.save(visit);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
                }
            });
        }
    }



    // ✅ २. डॉक्टरसाठी (Main Logic सह नवीन मेथड)
    public List<Map<String, Object>> getTodayQueue(Long clinicId, LocalDate date, Long doctorId) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        // रांगेत दिसण्यासाठी स्टेटस फिल्टर्स
        List<VisitStatus> activeStatuses = Arrays.asList(
                VisitStatus.ARRIVED,
                VisitStatus.BILLING_PENDING,
                VisitStatus.COMPLETED
        );

        List<Visit> queue;
        if (doctorId != null) {
            log.info("🏥 Fetching Queue for Doctor ID: {} at Clinic: {}", doctorId, clinicId);
            // 🔥 Doctor-wise Filter (Repository मध्ये ही मेथड ॲड करा)
            queue = visitRepository.findByVisitDateAndClinicIdAndDoctorIdAndStatusInOrderByEmergencyDescTokenNumberAsc(
                    queryDate, clinicId, doctorId, activeStatuses
            );
        } else {
            log.info("🏢 Fetching Full Clinic Queue for Clinic ID: {}", clinicId);
            // रिसेप्शनिस्टसाठी सर्व पेशंट्स
            queue = visitRepository.findByVisitDateAndClinicIdAndStatusInOrderByEmergencyDescTokenNumberAsc(
                    queryDate, clinicId, activeStatuses
            );
        }

        return queue.stream().map(this::mapToQueueMap).collect(Collectors.toList());
    }

    // ✅ ३. मॅपिंग हेल्पपर (Queue साठी)
    private Map<String, Object> mapToQueueMap(Visit v) {
        Map<String, Object> map = new HashMap<>();
        map.put("visitId", v.getId());
        map.put("tokenNumber", v.getTokenNumber());
        map.put("patientName", v.getPatient().getName());
        map.put("visitType", v.getVisitType());
        map.put("status", v.getStatus());
        map.put("isEmergency", v.isEmergency());
        map.put("doctorName", v.getDoctor().getName());
        map.put("mobile", v.getPatient().getMobile());

        // Completion Time जर व्हिजिट पूर्ण झाली असेल तर
        if (v.getStatus() == VisitStatus.COMPLETED && v.getCompletionDateTime() != null) {
            map.put("time", v.getCompletionDateTime());
        } else {
            map.put("time", "In Queue");
        }

        return map;
    }

    public List<Map<String, Object>> getTodayFollowUps(Long clinicId, Long doctorId) {
        LocalDate today = LocalDate.now();
        List<Visit> followUps = visitRepository.findTodayFollowUps(today, clinicId, doctorId);

        return followUps.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("patientId", v.getPatient().getId());
            map.put("patientName", v.getPatient().getName());
            map.put("mobile", v.getPatient().getMobile());
            map.put("lastDiagnosis", v.getDiagnosis());
            map.put("lastVisitDate", v.getVisitDate());
            return map;
        }).collect(Collectors.toList());
    }
}