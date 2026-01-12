package com.prajyotsurgicare.clinic.dto;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DirectCheckupRequest {
    private Long patientId;
    private Long doctorId;
    private Long clinicId;

    // Medical
    private String diagnosis;
    private String prescription;

    // Vitals
    private String bp;
    private String weight;
    private String temperature;

    // Billing
    private Double consultationFee;
    private Double otherCharges;
    private Double totalAmount;

    private LocalDate followUpDate;
}