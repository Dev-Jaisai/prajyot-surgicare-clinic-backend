package com.prajyotsurgicare.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PatientListResponse {
    private Long id;
    private String name;
    private String mobile;
    private String gender;// ✅ Add this
    private Integer age; // ✅ Add this
}
