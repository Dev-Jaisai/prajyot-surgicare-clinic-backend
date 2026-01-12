package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.DirectCheckupRequest;
import com.prajyotsurgicare.clinic.dto.PatientRegistrationRequest;
import com.prajyotsurgicare.clinic.dto.VisitResponse;
import com.prajyotsurgicare.clinic.entity.Patient;
import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.service.PatientService;
import com.prajyotsurgicare.clinic.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/visits") // ✅ Frontend ची हीच URL कॉल होईल
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;
    private final PatientService patientService;

    // 🔥 EXISTING PATIENT VISIT (जेव्हा डॉक्टर डॅशबोर्डवरून जुना पेशंट सिलेक्ट करतात)
    @PostMapping
    public ResponseEntity<VisitResponse> createVisit(@RequestBody PatientRegistrationRequest request) {

        log.info("🏥 Adding New Visit for Existing Patient ID: {}", request.getPatientId());

        // 1. Check if Patient ID is present
        if (request.getPatientId() == null) {
            throw new RuntimeException("Patient ID is required to create a visit!");
        }

        // 2. जुना पेशंट डेटाबेस मधून आणा (Reusing PatientService)
        Patient patient = patientService.getPatientById(request.getPatientId());

        // 3. क्लिनिक आयडी घ्या (डिफॉल्ट 1 जर नसेल तर)
        Long clinicId = request.getClinicId() != null ? request.getClinicId() : 1L;

        // 4. डॉक्टर आयडी चेक करा (जर नसेल तर डिफॉल्ट 1 - Ortho)
        if (request.getDoctorId() == null) {
            request.setDoctorId(1L);
        }

        // 5. नवीन व्हिजिट तयार करा (Reusing VisitService - सेम लॉजिक!)
        Visit visit = visitService.createVisitWithClinic(patient, request, clinicId);

        // 6. रिस्पॉन्स द्या
        return ResponseEntity.ok(VisitResponse.builder()
                .visitId(visit.getId())
                .status(visit.getStatus())
                .tokenNumber(visit.getTokenNumber())
                .doctorName(visit.getDoctor().getName())
                .visitDate(visit.getVisitDate())
                .build());
    }

    @PostMapping("/direct-checkup")
    public ResponseEntity<Long> createDirectCheckup(@RequestBody DirectCheckupRequest request) {
        Long visitId = visitService.processDirectCheckup(request);
        return ResponseEntity.ok(visitId);
    }
}