package com.prajyotsurgicare.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String name;      // ✅ New
    private Long doctorId;    // ✅ New
    private Long clinicId;    // ✅ New
}