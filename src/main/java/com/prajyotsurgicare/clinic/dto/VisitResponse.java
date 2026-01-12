package com.prajyotsurgicare.clinic.dto;

import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.enums.VisitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitResponse {
    private Long visitId;
    private String patientName;
    private VisitType visitType;
    private VisitStatus status;
    private boolean isEmergency;
    private String reason;
    private LocalDate visitDate;

    private String clinicName;
    private String doctorName;

    private String diagnosis;

    // ✅ NEW: हे फील्ड ॲड करा (रिसेप्शनिस्टसाठी)
    private String prescription;

    private Double totalAmount;
    private boolean hasFile;
    private LocalDate followUpDate;
    private Integer tokenNumber;
}