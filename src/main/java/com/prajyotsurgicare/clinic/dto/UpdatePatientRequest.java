package com.prajyotsurgicare.clinic.dto;

import com.prajyotsurgicare.clinic.enums.Gender;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePatientRequest {
    private String name;
    private Gender gender;
    private Integer age;
    private String address;
}
