package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.dto.PatientRegistrationRequest;
import com.prajyotsurgicare.clinic.dto.PatientResponse;
import com.prajyotsurgicare.clinic.dto.UpdatePatientRequest;
import com.prajyotsurgicare.clinic.entity.Patient;
import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.exception.BadRequestException;
import com.prajyotsurgicare.clinic.mapper.PatientMapper;
import com.prajyotsurgicare.clinic.repository.PatientRepository;
import com.prajyotsurgicare.clinic.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j // ✅ Logging ON
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final VisitRepository visitRepository;

    public Patient getOrCreatePatient(PatientRegistrationRequest request) {
        String mobile = request.getMobile();
        String name = request.getName().trim();
        log.info("🔍 Checking patient: Mobile={}, Name={}", mobile, name);

        Optional<Patient> existingPatient = patientRepository.findByMobileAndNameIgnoreCase(mobile, name);

        if (existingPatient.isPresent()) {
            log.info("✅ Found Existing: ID={}", existingPatient.get().getId());
            return existingPatient.get();
        } else {
            log.info("🆕 Creating New Patient: {}", name);
            Patient newPatient = patientMapper.toNewPatient(request);
            return patientRepository.save(newPatient);
        }
    }
    // ✅ FIXED: Total Visits आणि Nearest follow-up दाखवतो
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        log.info("📋 Fetching Patients List Page...");

        return patientRepository.findAll(pageable)
                .map(patient -> {
                    // 🔥 १. पेशंटच्या एकूण व्हिजिट्स मोजा
                    long visitsCount = visitRepository.countByPatientId(patient.getId());

                    // 🔥 २. सर्व upcoming follow-ups शोधा
                    LocalDate nextFollowUp = visitRepository
                            .findNextFollowUpDate(patient.getId(), LocalDate.now())
                            .orElse(null);

                    if(nextFollowUp != null) {
                        log.info("📅 Patient: {} | Next Follow-up: {}",
                                patient.getName(), nextFollowUp);
                    }

                    return PatientResponse.builder()
                            .id(patient.getId())
                            .name(patient.getName())
                            .mobile(patient.getMobile())
                            .gender(patient.getGender() != null ? patient.getGender().name() : null)
                            .age(patient.getAge())
                            .totalVisits((int) visitsCount) // ✅ आता 0 ऐवजी Actual Count दिसेल
                            .followUpDate(nextFollowUp)
                            .build();
                });
    }

    public Patient updatePatient(Long id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Patient not found"));
        if (request.getName() != null) patient.setName(request.getName());
        if (request.getGender() != null) patient.setGender(request.getGender());
        if (request.getAge() != null) patient.setAge(request.getAge());
        if (request.getAddress() != null) patient.setAddress(request.getAddress());
        return patientRepository.save(patient);
    }

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Patient not found"));
    }

    public List<Patient> searchPatients(String query) {
        if (query.matches("\\d+")) {
            return patientRepository.findByMobileContaining(query);
        }
        return patientRepository.findByNameContainingIgnoreCase(query);
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Patient not found"));
        visitRepository.deleteByPatientId(id);
        patientRepository.delete(patient);
        log.info("Patient deleted. ID={}", id);
    }



    // ✅ NEW: Manual Follow-up Update (Receptionist Calendar)
    // हे फक्त 'Latest Visit' ची तारीख अपडेट करेल.
    // ✅ CORRECT: Existing Visit Update करा
    @Transactional
    public void updateFollowUp(Long patientId, LocalDate date) {
        // 1. पेशंटची आजची/शेवटची व्हिजिट शोधा
        Visit lastVisit = visitRepository.findTopByPatientIdOrderByVisitDateDesc(patientId)
                .orElseThrow(() -> new RuntimeException("No visits found"));

        // 2. त्याच व्हिजिटला तारीख जोडा (नवीन बनवू नका)
        lastVisit.setFollowUpDate(date);
        visitRepository.save(lastVisit);

        log.info("✅ Follow-up attached to Visit ID: {}", lastVisit.getId());
    }
}