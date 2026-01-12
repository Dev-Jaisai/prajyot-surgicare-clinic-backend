package com.prajyotsurgicare.clinic.mapper;

import com.prajyotsurgicare.clinic.dto.PatientRegistrationRequest;
import com.prajyotsurgicare.clinic.entity.Patient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PatientMapper {

    public Patient toNewPatient(PatientRegistrationRequest dto) {

        Patient patient = new Patient();
        patient.setName(dto.getName());
        patient.setMobile(dto.getMobile());
        patient.setGender(dto.getGender());
        patient.setAge(dto.getAge());
        patient.setAddress(dto.getAddress());
        patient.setCreatedAt(LocalDateTime.now());

        return patient;
    }
}
