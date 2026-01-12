package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.BillingRequest;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import com.prajyotsurgicare.clinic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
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
}