package com.prajyotsurgicare.clinic.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.prajyotsurgicare.clinic.dto.PrescriptionView;
import com.prajyotsurgicare.clinic.entity.PrescriptionFile;
import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.repository.PrescriptionFileRepository;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List; // 🔥 Explicitly Import Java List
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionFileRepository fileRepository;
    private final VisitRepository visitRepository;

    // 1. 👨‍⚕️ DOCTOR: Generate PDF from Text & Stylus
    @Transactional
    public void generateAndSavePdf(Long visitId, String textNote, String stylusImageBase64) {
        try {
            Visit visit = visitRepository.findById(visitId).orElseThrow();

            // 1. Create PDF Document
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 2. Add Header (Clinic Name)
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph header = new Paragraph("PRAJYOT SURGICARE CLINIC", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            document.add(new Paragraph("\n")); // Space

            // 3. Add Patient & Doctor Details
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Date: " + visit.getVisitDate(), boldFont));
            document.add(new Paragraph("Doctor: " + visit.getDoctor().getName()));
            document.add(new Paragraph("Patient: " + visit.getPatient().getName()));
            document.add(new Paragraph("-------------------------------------------------------------------"));
            document.add(new Paragraph("\n"));

            // 4. Add Typed Text (Diagnosis/Meds)
            if (textNote != null && !textNote.isEmpty()) {
                document.add(new Paragraph("Prescription / Notes:", boldFont));
                document.add(new Paragraph(textNote));
                document.add(new Paragraph("\n"));
            }

            // 5. Add Stylus Image (Signature/Drawing)
            if (stylusImageBase64 != null && !stylusImageBase64.isEmpty()) {
                // Base64 to Image
                String base64Data = stylusImageBase64.split(",")[1]; // Remove "data:image/png;base64," header
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                Image img = Image.getInstance(imageBytes);
                img.scaleToFit(500, 400); // Resize if too big
                img.setAlignment(Element.ALIGN_CENTER);
                document.add(img);
            }

            // 6. Footer
            document.add(new Paragraph("\n\n\n"));
            Paragraph footer = new Paragraph("(Digitally Generated Prescription)", FontFactory.getFont(FontFactory.HELVETICA, 8));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // 7. Save to DB
            saveFileToDb(visit, out.toByteArray(), "prescription.pdf", "application/pdf");

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    // 2. 👩‍💼 RECEPTIONIST: Upload Manual Photo
    @Transactional
    public void uploadPrescriptionImage(Long visitId, MultipartFile file) throws IOException {
        Visit visit = visitRepository.findById(visitId).orElseThrow();
        saveFileToDb(visit, file.getBytes(), file.getOriginalFilename(), file.getContentType());
    }

    // ✅✅ FIXED: Multiple File Support Logic
    // जुने लॉजिक (Find & Replace) काढले आहे. आता प्रत्येक वेळी नवीन फाईल बनेल.
    private void saveFileToDb(Visit visit, byte[] data, String name, String type) {

        PrescriptionFile file = new PrescriptionFile(); // 🔥 Always New Object

        file.setVisit(visit);
        file.setData(data);
        file.setFileName(name);
        file.setFileType(type);

        // जर Entity मध्ये uploadedAt फील्ड असेल तर ते सेट करा (Optional)
        // file.setUploadedAt(LocalDateTime.now());

        fileRepository.save(file); // 🔥 New Entry Created
    }

    // 3. ✅ History: Get Recent Prescriptions
    public List<PrescriptionView> getRecentPrescriptions(Long patientId) {
        List<PrescriptionFile> files = fileRepository.findRecentByPatientId(patientId);

        return files.stream()
                .limit(10) // Last 10 prescriptions
                .map(file -> {
                    // Extract date safely
                    String dateStr = (file.getVisit() != null && file.getVisit().getVisitDate() != null)
                            ? file.getVisit().getVisitDate().toString()
                            : "Unknown Date";

                    return new PrescriptionView(
                            file.getId(),
                            dateStr, // Maps to 'date' field in DTO
                            file.getFileName()
                    );
                })
                .collect(Collectors.toList());
    }

    // 4. ✅ View File: Get Single File Content
    public PrescriptionFile getFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Prescription file not found with id: " + fileId));
    }

    // 5. ✅ List: Get All IDs for a Visit
    public List<Long> getPrescriptionIds(Long visitId) {
        return fileRepository.findAllByVisitId(visitId)
                .stream()
                .map(PrescriptionFile::getId)
                .collect(Collectors.toList());
    }

    // 6. ⚠️ Angular Compatibility: Get Latest File for Visit (Old Logic Support)
    public PrescriptionFile getLatestFileByVisitId(Long visitId) {
        List<PrescriptionFile> files = fileRepository.findAllByVisitId(visitId);
        if (files.isEmpty()) {
            throw new RuntimeException("No file found for visit " + visitId);
        }
        // शेवटची (Latest) फाईल रिटर्न करा (List मधील शेवटचा एलिमेंट)
        return files.get(files.size() - 1);
    }
}