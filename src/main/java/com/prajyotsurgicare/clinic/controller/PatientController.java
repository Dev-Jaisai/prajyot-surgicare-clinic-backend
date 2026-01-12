package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.*;
import com.prajyotsurgicare.clinic.entity.Patient;
import com.prajyotsurgicare.clinic.repository.PatientRepository;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import com.prajyotsurgicare.clinic.service.PatientService;
import com.prajyotsurgicare.clinic.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final VisitService visitService;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;

    // ⚠️ OLD / LEGACY API
    @PostMapping("/register")
    public ResponseEntity<PatientResponse> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {

        log.info("Legacy registration request for mobile={}", request.getMobile());

        Patient patient = patientService.getOrCreatePatient(request);
        visitService.createVisitWithClinic(patient, request, 1L);
        long totalVisits = visitRepository.countByPatientId(patient.getId());

        return ResponseEntity.ok(mapToResponse(patient, totalVisits));
    }

  /*  // ✅ NEW API
    @PostMapping("/register-with-clinic")
    public ResponseEntity<PatientResponse> registerPatientWithClinic(
            @RequestHeader("X-CLINIC-ID") Long clinicId,
            @Valid @RequestBody PatientRegistrationRequest request) {

        Patient patient = patientService.getOrCreatePatient(request);
        visitService.createVisitWithClinic(patient, request, clinicId);
        long totalVisits = visitRepository.countByPatientId(patient.getId());

        return ResponseEntity.ok(mapToResponse(patient, totalVisits));
    }*/
  @PostMapping("/register-with-clinic")
  public ResponseEntity<PatientResponse> registerPatientWithClinic(
          @RequestHeader("X-CLINIC-ID") Long clinicId,
          @Valid @RequestBody PatientRegistrationRequest request) {

      Patient patient = patientService.getOrCreatePatient(request);

      // 🔥🔥 फक्त जर Flag 'TRUE' असेल तरच व्हिजिट बनवा 🔥🔥
      if (request.isCreateVisit()) {
          visitService.createVisitWithClinic(patient, request, clinicId);
      }
      // -----------------------------------------------------------

      long totalVisits = visitRepository.countByPatientId(patient.getId());
      return ResponseEntity.ok(mapToResponse(patient, totalVisits));
  }
    @GetMapping("/search")
    public ResponseEntity<List<PatientResponse>> searchPatients(@RequestParam String query) {
        List<Patient> patients = patientService.searchPatients(query);
        return ResponseEntity.ok(
                patients.stream()
                        .map(p -> mapToResponse(p, visitRepository.countByPatientId(p.getId())))
                        .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @RequestBody UpdatePatientRequest request) {
        Patient patient = patientService.updatePatient(id, request);
        long visits = visitRepository.countByPatientId(patient.getId());
        return ResponseEntity.ok(mapToResponse(patient, visits));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id) {
        Patient patient = patientService.getPatientById(id);
        long visits = visitRepository.countByPatientId(patient.getId());
        return ResponseEntity.ok(mapToResponse(patient, visits));
    }
    // 🔥 HELPER METHOD (✅ FULLY FIXED)
    private PatientResponse mapToResponse(Patient patient, long totalVisits) {

        // ✅ CHANGE: Pass 'LocalDate.now()' as the second argument!
        // कारण आपण Repository मध्ये @Param("today") ॲड केले आहे.
        LocalDate nextFollowUp = visitRepository
                .findNextFollowUpDate(patient.getId(), LocalDate.now())
                .orElse(null);

        return PatientResponse.builder()
                .id(patient.getId())
                .name(patient.getName())
                .mobile(patient.getMobile())
                .totalVisits((int) totalVisits)
                .gender(patient.getGender() != null ? patient.getGender().name() : null)
                .age(patient.getAge())
                .followUpDate(nextFollowUp)
                .build();
    }

    // ---------------- OTHER METHODS ----------------

    @GetMapping("/{patientId}/visits")
    public ResponseEntity<List<VisitResponse>> getVisitHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(visitService.getVisitHistory(patientId));
    }

    @GetMapping("/stats/today")
    public ResponseEntity<Long> todayRegistrations() {
        long count = visitRepository.countByVisitDate(LocalDate.now());
        return ResponseEntity.ok(count);
    }

    @GetMapping
    public ResponseEntity<Page<PatientResponse>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                patientService.getAllPatients(PageRequest.of(page, size))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search-mobile/{mobile}")
    public ResponseEntity<List<Patient>> getPatientsByMobile(@PathVariable String mobile) {
        List<Patient> patients = patientRepository.findAllByMobile(mobile);
        return ResponseEntity.ok(patients);
    }

    @PutMapping("/{id}/follow-up")
    public ResponseEntity<?> updateFollowUp(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String dateStr = request.get("date");
        if (dateStr != null) {
            patientService.updateFollowUp(id, LocalDate.parse(dateStr));
            return ResponseEntity.ok("Follow-up updated");
        }
        return ResponseEntity.badRequest().body("Date is required");
    }

}