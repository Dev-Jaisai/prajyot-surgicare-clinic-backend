package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.BillingRequest;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import com.prajyotsurgicare.clinic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final VisitRepository visitRepository;

    @GetMapping("/stats/waiting/clinic")
    public ResponseEntity<Long> getWaitingCount(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(dashboardService.getWaitingCount(clinicId, date));
    }

    // ✅ FIXED: फक्त ही एकच मेथड ठेवा जी डॉक्टर आणि रिसेप्शनिस्ट दोघांसाठी चालेल
    @GetMapping("/queue/clinic")
    public ResponseEntity<List<Map<String, Object>>> getQueue(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long doctorId
    ) {
        return ResponseEntity.ok(dashboardService.getTodayQueue(clinicId, date, doctorId));
    }

    @GetMapping("/appointments/clinic")
    public ResponseEntity<List<Map<String, Object>>> getAppointments(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(dashboardService.getBookedAppointments(clinicId, date));
    }

    @GetMapping("/completed/clinic")
    public ResponseEntity<List<Map<String, Object>>> getCompleted(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(dashboardService.getCompletedVisits(clinicId, date));
    }

    @GetMapping("/stats/today/clinic")
    public ResponseEntity<Long> getTodayStats(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(visitRepository.countByVisitDateAndClinicId(queryDate, clinicId));
    }

    @PutMapping("/visit/{id}/arrive")
    public ResponseEntity<Void> markArrived(@PathVariable Long id) {
        dashboardService.markArrived(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/visit/{id}/complete")
    public ResponseEntity<Void> markCompleted(@PathVariable Long id, @RequestBody BillingRequest req) {
        dashboardService.markVisitAsCompleted(id, req);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/queue/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<Long> ids) {
        dashboardService.updateQueueOrder(ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/visit/{id}")
    public ResponseEntity<Map<String, Object>> getVisitDetails(@PathVariable Long id) {
        return ResponseEntity.ok(dashboardService.getVisitDetails(id));
    }

    @PutMapping("/visit/{id}/vitals")
    public ResponseEntity<Void> updateVitals(@PathVariable Long id, @RequestBody com.prajyotsurgicare.clinic.dto.VitalsRequest request) {
        dashboardService.updateVitals(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/visit/{id}/emergency")
    public ResponseEntity<Void> markEmergency(@PathVariable Long id) {
        dashboardService.markEmergency(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/visit/{visitId}/revert")
    public ResponseEntity<?> revertToBooked(@PathVariable Long visitId) {
        dashboardService.revertToBooked(visitId);
        return ResponseEntity.ok().build();
    }
    // 🔥 NEW: Today's Complete Stats (Collection + Patients + Waiting)
    @GetMapping("/stats/today")
    public ResponseEntity<Map<String, Object>> getTodayStats(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam(required = false) Long doctorId
    ) {
        LocalDate today = LocalDate.now();

        log.info("📊 Fetching Today's Stats for Clinic: {}, Doctor: {}", clinicId, doctorId);

        Map<String, Object> stats = new HashMap<>();

        try {
            if (doctorId != null) {
                // 🏥 Doctor-specific stats
                stats.put("totalPatients", getDoctorTotalPatients(today, clinicId, doctorId));
                stats.put("totalCollection", getDoctorTotalCollection(today, clinicId, doctorId));
                stats.put("waitingCount", getDoctorWaitingCount(today, clinicId, doctorId));
                stats.put("completedCount", getDoctorCompletedCount(today, clinicId, doctorId));
            } else {
                // 🏢 Clinic-wide stats (for Receptionist)
                stats.put("totalPatients", getClinicTotalPatients(today, clinicId));
                stats.put("totalCollection", getClinicTotalCollection(today, clinicId));
                stats.put("waitingCount", getClinicWaitingCount(today, clinicId));
                stats.put("completedCount", getClinicCompletedCount(today, clinicId));
            }

            log.info("✅ Stats Retrieved: {}", stats);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("❌ Error fetching stats: {}", e.getMessage());
            // Return zeros on error
            stats.put("totalPatients", 0L);
            stats.put("totalCollection", 0.0);
            stats.put("waitingCount", 0L);
            stats.put("completedCount", 0L);
            return ResponseEntity.ok(stats);
        }
    }

    // ==================== CLINIC-WIDE METHODS ====================

    private long getClinicTotalPatients(LocalDate date, Long clinicId) {
        // आजचे सर्व visits (सर्व statuses)
        return visitRepository.countByVisitDateAndClinicId(date, clinicId);
    }

    private Double getClinicTotalCollection(LocalDate date, Long clinicId) {
        // आजचे completed visits चे एकूण पैसे
        Double collection = visitRepository.getDailyCollection(date, clinicId);
        return collection != null ? collection : 0.0;
    }

    private long getClinicWaitingCount(LocalDate date, Long clinicId) {
        // आजचे ARRIVED status visits
        return visitRepository.countByVisitDateAndClinicIdAndStatus(
                date, clinicId, VisitStatus.ARRIVED
        );
    }

    private long getClinicCompletedCount(LocalDate date, Long clinicId) {
        // आजचे COMPLETED status visits
        return visitRepository.countByVisitDateAndClinicIdAndStatus(
                date, clinicId, VisitStatus.COMPLETED
        );
    }

    // ==================== DOCTOR-SPECIFIC METHODS ====================

    private long getDoctorTotalPatients(LocalDate date, Long clinicId, Long doctorId) {
        // या डॉक्टरचे आजचे सर्व visits
        return visitRepository.countByVisitDateAndClinicIdAndDoctorId(
                date, clinicId, doctorId
        );
    }

    private Double getDoctorTotalCollection(LocalDate date, Long clinicId, Long doctorId) {
        // या डॉक्टरचे completed visits चे पैसे
        Double collection = visitRepository.getDailyCollectionByDoctor(
                date, clinicId, doctorId
        );
        return collection != null ? collection : 0.0;
    }

    private long getDoctorWaitingCount(LocalDate date, Long clinicId, Long doctorId) {
        // या डॉक्टरचे waiting patients
        return visitRepository.countByVisitDateAndClinicIdAndDoctorIdAndStatus(
                date, clinicId, doctorId, VisitStatus.ARRIVED
        );
    }

    private long getDoctorCompletedCount(LocalDate date, Long clinicId, Long doctorId) {
        // या डॉक्टरचे completed patients
        return visitRepository.countByVisitDateAndClinicIdAndDoctorIdAndStatus(
                date, clinicId, doctorId, VisitStatus.COMPLETED
        );
    }
}