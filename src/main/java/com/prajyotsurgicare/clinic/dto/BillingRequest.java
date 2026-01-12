package com.prajyotsurgicare.clinic.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BillingRequest {
    // ✅ हे जुने आहेत (तसेच ठेवा)
    private Double consultationFee;
    private Double otherCharges;
    private String paymentMode;
    private String procedures;

    // ✅ हे नवीन ॲड केले (Follow-up आणि Total Bill साठी)
    private Long visitId;
    private Double amount;          // Grand Total (Fee + Charges)
    private LocalDate followUpDate; // 📅 Next Visit Date
}