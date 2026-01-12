package com.prajyotsurgicare.clinic.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalInfoRequest {
    private String diagnosis;
    private String prescription;
    private Double otherCharges;
    private LocalDate followUpDate;

    // ✅ NEW FIELD: Solo Doctor Mode साठी
    private Boolean paymentCollected;
}