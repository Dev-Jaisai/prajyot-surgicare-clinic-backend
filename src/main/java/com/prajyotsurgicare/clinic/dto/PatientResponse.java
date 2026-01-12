package com.prajyotsurgicare.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder; // ✅ Import Added
import lombok.Data;      // ✅ Data is better than Getter/Setter
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data // ✅ Getters, Setters, toString, hashCode
@Builder // ✅ Builder Pattern साठी
@AllArgsConstructor
@NoArgsConstructor
public class PatientResponse {

    private Long id;
    private String name;
    private String mobile;
    private int totalVisits;
    private String gender;
    private Integer age;
    private LocalDate followUpDate; // ✅ Correct
}