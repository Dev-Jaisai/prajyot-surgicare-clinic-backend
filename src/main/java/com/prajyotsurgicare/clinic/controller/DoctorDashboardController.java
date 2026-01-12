package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.MedicalHistoryResponse;
import com.prajyotsurgicare.clinic.dto.MedicalInfoRequest;
import com.prajyotsurgicare.clinic.service.DoctorDashboardService; // ✅ New Service Used
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorDashboardController { // ✅ Renamed Class

    private final DoctorDashboardService doctorService; // ✅ Using specific service

    // 1. GET QUEUE
    @GetMapping("/my-queue")
    public ResponseEntity<List<Map<String, Object>>> getDoctorQueue(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam(required = false) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        // Logic service मध्ये हलवले आहे
        return ResponseEntity.ok(doctorService.getDoctorQueue(clinicId, doctorId, queryDate));
    }

    // 2. UPDATE INFO
    @PutMapping("/visit/{id}/medical-info")
    public ResponseEntity<Void> updateMedicalInfo(
            @PathVariable Long id,
            @RequestBody MedicalInfoRequest request) {

        doctorService.updateMedicalDetails(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/visit/{id}/complete")
    public ResponseEntity<Void> completeCheckup(
            @PathVariable Long id,
            @RequestBody MedicalInfoRequest request) {

        // ✅ हे Service मधील नवीन मेथडला कॉल करेल
        doctorService.completeCheckup(id, request);
        return ResponseEntity.ok().build();
    }
// DoctorDashboardController.java मध्ये ॲड करा:

//    @GetMapping("/patient/{patientId}/history")
//    public ResponseEntity<List<Map<String, Object>>> getPatientHistory(@PathVariable Long patientId) {
//        return ResponseEntity.ok(doctorService.getPatientHistory(patientId));
//    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<MedicalHistoryResponse>> getHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(doctorService.getPatientHistory(patientId));
    }
}