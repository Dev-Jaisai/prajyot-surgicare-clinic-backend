package com.prajyotsurgicare.clinic.mapper;

import com.prajyotsurgicare.clinic.dto.PatientRegistrationRequest;
import com.prajyotsurgicare.clinic.entity.Patient;
import com.prajyotsurgicare.clinic.entity.Visit;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class VisitMapper {

    public Visit toVisit(Patient patient, PatientRegistrationRequest dto) {

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitType(dto.getVisitType());
        visit.setReason(dto.getReason());
        visit.setVisitDate(LocalDate.now());
        visit.setCreatedAt(LocalDateTime.now());
        visit.setQueueOrder((int) System.currentTimeMillis());
        visit.setEmergency(dto.isEmergency());
        return visit;
    }
}
