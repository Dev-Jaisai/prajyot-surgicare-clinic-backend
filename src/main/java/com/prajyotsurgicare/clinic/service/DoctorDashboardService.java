package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.dto.MedicalHistoryResponse;
import com.prajyotsurgicare.clinic.dto.MedicalInfoRequest;
import com.prajyotsurgicare.clinic.entity.PrescriptionFile;
import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.enums.VisitType; // ✅ Import VisitType
import com.prajyotsurgicare.clinic.repository.PrescriptionFileRepository;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorDashboardService {

    private final PrescriptionFileRepository fileRepository;
    private final WebSocketService webSocketService;
    private final VisitRepository visitRepository;
    private final NotificationService notificationService;

    // 🔥🔥 NEW: PrescriptionService Inject केला
    private final PrescriptionService prescriptionService;

    public List<Map<String, Object>> getDoctorQueue(Long clinicId, Long doctorId, LocalDate date) {
        List<VisitStatus> activeStatuses = Arrays.asList(
                VisitStatus.ARRIVED,
                VisitStatus.IN_PROGRESS
        );

        List<Visit> visits = visitRepository
                .findByVisitDateAndClinicIdAndStatusInOrderByEmergencyDescQueueOrderAsc(
                        date, clinicId, activeStatuses
                );

        return visits.stream()
                .filter(v -> {
                    if (v.getDoctor() == null) return false;
                    if (doctorId == 0) return true;
                    return v.getDoctor().getId().equals(doctorId);
                })
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("visitId", v.getId());
                    map.put("tokenNumber", v.getTokenNumber());
                    map.put("patientName", v.getPatient().getName());
                    map.put("doctorName", v.getDoctor().getName());
                    map.put("visitType", v.getVisitType());
                    map.put("patientId", v.getPatient().getId());
                    map.put("bp", v.getBp());
                    map.put("temp", v.getTemperature());
                    map.put("weight", v.getWeight());
                    map.put("isEmergency", v.isEmergency());

                    List<PrescriptionFile> files = fileRepository.findAllByVisitId(v.getId());
                    map.put("hasFile", !files.isEmpty());

                    return map;
                })
                .toList();
    }

    @Transactional
    public void completeCheckup(Long visitId, MedicalInfoRequest request) {
        log.info("🏥 Completing Checkup for Visit ID: {}", visitId);

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        // 1. डेटा अपडेट करा
        visit.setDiagnosis(request.getDiagnosis());
        visit.setPrescriptionNote(request.getPrescription());

        if (request.getOtherCharges() != null) {
            visit.setOtherCharges(request.getOtherCharges());
        }

        if (request.getFollowUpDate() != null) {
            visit.setFollowUpDate(request.getFollowUpDate());
        }

        // 2. स्टेटस अपडेट करा
        if (Boolean.TRUE.equals(request.getPaymentCollected())) {
            visit.setStatus(VisitStatus.COMPLETED);
            Double consultation = visit.getConsultationFee() != null ? visit.getConsultationFee() : 500.0;
            Double other = request.getOtherCharges() != null ? request.getOtherCharges() : 0.0;

            visit.setConsultationFee(consultation);
            visit.setTotalAmount(consultation + other);
            visit.setPaidAmount(consultation + other);
            visit.setPaymentMode("CASH");
            visit.setCompletionDateTime(LocalDateTime.now());
            visit.setPaymentCollected(true);

        } else {
            visit.setStatus(VisitStatus.BILLING_PENDING);
        }

        visitRepository.save(visit);

       /* // 🔥🔥🔥 MAIN FIX: जर On-Call असेल तर PDF बनवा आणि DB मध्ये सेव्ह करा 🔥🔥🔥
        if (visit.getVisitType() == VisitType.ON_CALL) {
            try {
                log.info("📄 Generating PDF for On-Call Visit ID: {}", visitId);
                // null means no stylus drawing (signature), just text
                prescriptionService.generateAndSavePdf(visitId, request.getPrescription(), null);
            } catch (Exception e) {
                log.error("❌ Failed to generate PDF: {}", e.getMessage());
            }
        }*/
        // -------------------------------------------------------------

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketService.sendQueueUpdate(visit.getClinic().getId(), "REFRESH_QUEUE");
                if (visit.getStatus() == VisitStatus.COMPLETED && visit.getPatient().getMobile() != null) {
                    notificationService.sendThankYouMessage(visit.getPatient().getName(), visit.getPatient().getMobile());
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public List<MedicalHistoryResponse> getPatientHistory(Long patientId) {
        return visitRepository.findByPatientIdOrderByVisitDateDesc(patientId)
                .stream()
                .map(v -> {
                    List<PrescriptionFile> files = fileRepository.findAllByVisitId(v.getId());
                    boolean hasFile = !files.isEmpty();

                    String docName = (v.getDoctor() != null) ? v.getDoctor().getName() : "Unknown";
                    String clinicName = (v.getClinic() != null) ? v.getClinic().getName() : "Unknown";

                    return MedicalHistoryResponse.builder()
                            .visitId(v.getId())
                            .visitDate(v.getVisitDate())
                            .diagnosis(v.getDiagnosis())
                            .prescription(v.getPrescriptionNote())
                            .bp(v.getBp())
                            .weight(v.getWeight())
                            .temp(v.getTemperature())
                            .hasFile(hasFile)
                            .followUpDate(v.getFollowUpDate())
                            .doctorName(docName)
                            .clinicName(clinicName)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void updateMedicalDetails(Long visitId, MedicalInfoRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        visit.setDiagnosis(request.getDiagnosis());
        if (request.getOtherCharges() != null) {
            visit.setOtherCharges(request.getOtherCharges());
        }
        visitRepository.save(visit);
    }
}