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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionFileRepository fileRepository;
    private final VisitRepository visitRepository;

    // =================================================================
    // 1. 👨‍⚕️ DOCTOR: Generate PDF from Text & Stylus
    // =================================================================
    @Transactional
    public void generateAndSavePdf(Long visitId, String textNote, String stylusImageBase64) {
        try {
            Visit visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new RuntimeException("Visit not found: " + visitId));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph header = new Paragraph("PRAJYOT SURGICARE CLINIC", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            document.add(new Paragraph("\n"));

            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Date: " + visit.getVisitDate(), boldFont));
            document.add(new Paragraph("Doctor: " + visit.getDoctor().getName()));
            document.add(new Paragraph("Patient: " + visit.getPatient().getName()));
            document.add(new Paragraph("-------------------------------------------------------------------"));
            document.add(new Paragraph("\n"));

            if (textNote != null && !textNote.isEmpty()) {
                document.add(new Paragraph("Prescription / Notes:", boldFont));
                document.add(new Paragraph(textNote));
                document.add(new Paragraph("\n"));
            }

            if (stylusImageBase64 != null && !stylusImageBase64.isEmpty()) {
                try {
                    String base64Data = stylusImageBase64.contains(",")
                            ? stylusImageBase64.split(",")[1]
                            : stylusImageBase64;
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    Image img = Image.getInstance(imageBytes);
                    img.scaleToFit(500, 400);
                    img.setAlignment(Element.ALIGN_CENTER);
                    document.add(img);
                } catch (Exception e) {
                    System.err.println("Error adding image to PDF: " + e.getMessage());
                }
            }

            document.add(new Paragraph("\n\n\n"));
            Paragraph footer = new Paragraph("(Digitally Generated Prescription)", FontFactory.getFont(FontFactory.HELVETICA, 8));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            saveFileToDb(visit, out.toByteArray(), "Prescription_" + visitId + ".pdf", "application/pdf");

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    // =================================================================
    // 2. 👩‍💼 RECEPTIONIST / DOCTOR: Upload Manual Photo
    // =================================================================
    @Transactional
    public void uploadPrescriptionImage(Long visitId, MultipartFile file) throws IOException {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found: " + visitId));
        saveFileToDb(visit, file.getBytes(), file.getOriginalFilename(), file.getContentType());
    }

    private void saveFileToDb(Visit visit, byte[] data, String name, String type) {
        PrescriptionFile file = new PrescriptionFile();
        file.setVisit(visit);
        file.setData(data);
        file.setFileName(name);
        file.setFileType(type);
        file.setUploadedAt(LocalDateTime.now());
        fileRepository.save(file);
    }

    // =================================================================
    // 3. ✅ GET DATA METHODS
    // =================================================================

    @Transactional(readOnly = true)
    public PrescriptionFile getFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Prescription file not found with id: " + fileId));
    }

    public List<Long> getPrescriptionIds(Long visitId) {
        return fileRepository.findAllByVisitId(visitId)
                .stream()
                .map(PrescriptionFile::getId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionView> getRecentPrescriptions(Long patientId) {
        List<PrescriptionFile> files = fileRepository.findRecentByPatientId(patientId);
        return files.stream()
                .limit(10)
                .map(file -> {
                    String dateStr = (file.getVisit() != null && file.getVisit().getVisitDate() != null)
                            ? file.getVisit().getVisitDate().toString()
                            : "Unknown Date";
                    return new PrescriptionView(file.getId(), dateStr, file.getFileName());
                })
                .collect(Collectors.toList());
    }

    // 🔥🔥 Fix: हे मिसिंग फंक्शन ॲड केले आहे
    @Transactional(readOnly = true)
    public PrescriptionFile getLatestFileByVisitId(Long visitId) {
        List<PrescriptionFile> files = fileRepository.findAllByVisitId(visitId);
        if (files.isEmpty()) {
            throw new RuntimeException("No file found for visit " + visitId);
        }
        // शेवटची फाईल रिटर्न करा
        return files.get(files.size() - 1);
    }
}