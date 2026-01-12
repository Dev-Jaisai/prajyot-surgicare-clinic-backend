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

    // 🩺 Vitals (हे फक्त डॉक्टरांसाठी असतात)
    private String bp;
    private String weight;
    private String temp;

    private boolean hasFile;
    private LocalDate followUpDate;

    // ✅ हे दोन नवीन फील्ड्स (महत्त्वाचे)
    private String doctorName;
    private String clinicName;
}