package com.prajyotsurgicare.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionView {
    private Long fileId;
    private String date;
    private String fileName;
}