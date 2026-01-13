package com.prajyotsurgicare.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicalHistoryResponse {

    private Long visitId;
    private LocalDate visitDate;
    private String diagnosis;
    private String prescription;

    // Vitals
    private String bp;
    private String weight;
    private String temp;

    // 🔥 हे फील्ड अत्यंत महत्त्वाचे आहे
    private boolean hasFile;

    private LocalDate followUpDate;
    private String doctorName;
    private String clinicName;

    // Visit Type पाठवणे गरजेचे आहे, जेणेकरून ऑरेंज टॅग दिसेल
    // private VisitType visitType; // (Optional but good for labels)
}