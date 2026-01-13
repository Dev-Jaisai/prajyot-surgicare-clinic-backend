package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.PrescriptionRequest;
import com.prajyotsurgicare.clinic.dto.PrescriptionView;
import com.prajyotsurgicare.clinic.entity.PrescriptionFile;
import com.prajyotsurgicare.clinic.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // ✅ Import हे ॲड केले
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // ✅ NEW: Get Recent Prescriptions for Patient (History)
    @GetMapping("/patient/{patientId}/recent")
    public ResponseEntity<List<PrescriptionView>> getRecentPrescriptions(@PathVariable Long patientId) {
        return ResponseEntity.ok(prescriptionService.getRecentPrescriptions(patientId));
    }

    // 1. 👨‍⚕️ DOCTOR: Generate PDF (Stylus + Text)
    @PostMapping("/{visitId}/generate")
    public ResponseEntity<Void> generatePdf(@PathVariable Long visitId, @RequestBody PrescriptionRequest request) {
        prescriptionService.generateAndSavePdf(visitId, request.getTextNote(), request.getImageBase64());
        return ResponseEntity.ok().build();
    }

    // 2. 👩‍💼 RECEPTIONIST: Upload Photo
    @PostMapping(value = "/{visitId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadImage(@PathVariable Long visitId, @RequestParam("file") MultipartFile file) throws IOException {
        prescriptionService.uploadPrescriptionImage(visitId, file);
        return ResponseEntity.ok().build();
    }

    // 🔥🔥 CRITICAL FIX: Added @Transactional here
    // हे ॲड केल्यामुळे Postgres LOB एरर येणार नाही
    @GetMapping("/view/{fileId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> viewPrescription(@PathVariable Long fileId) {
        try {
            System.out.println("🔍 Attempting to fetch file ID: " + fileId);

            PrescriptionFile file = prescriptionService.getFile(fileId);

            System.out.println("✅ File found: " + file.getFileName());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .body(file.getData()); // इथेच एरर येत होता, आता @Transactional मुळे तो फिक्स होईल.
        } catch (RuntimeException e) {
            System.err.println("❌ File not found: " + fileId + " - " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ MISSING METHOD: Get List of Prescription File IDs for a Visit
    @GetMapping("/{visitId}/list")
    public ResponseEntity<List<Long>> getPrescriptionList(@PathVariable Long visitId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionIds(visitId));
    }

    // ⚠️ ANGULAR COMPATIBILITY (Old Endpoint)
    // 🔥🔥 Fix here as well
    @GetMapping("/{visitId}/view")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> viewPrescriptionByVisitId(@PathVariable Long visitId) {
        try {
            // Service मधून Visit ID नुसार लेटेस्ट फाईल आणा
            PrescriptionFile file = prescriptionService.getLatestFileByVisitId(visitId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(file.getFileType()))
                    .body(file.getData());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}