package com.prajyotsurgicare.clinic.repository;

import com.prajyotsurgicare.clinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMobile(String mobile);
    Optional<Patient> findByMobileAndNameIgnoreCase(String mobile, String name);

    boolean existsByMobile(String mobile);

    List<Patient> findAllByOrderByCreatedAtDesc();
    List<Patient> findByMobileContaining(String mobile);

    List<Patient> findByNameContainingIgnoreCase(String name);
    List<Patient> findAllByMobile(String mobile);
    @Modifying
    @Query("UPDATE Patient p SET p.followUpDate = :date WHERE p.id = :id")
    void updateFollowUpDateDirectly(@Param("id") Long id, @Param("date") LocalDate date);


    // 🔥 RAM-BAAN QUERY: हे एका झटक्यात सर्व काम करेल!
    // हे काय करते:
    // 1. Visits टेबलमध्ये जाऊन आजच्या किंवा पुढच्या तारखा शोधते.
    // 2. त्यातली सर्वात लहान (MIN) तारीख निवडते.
    // 3. आणि ती Patient टेबलच्या follow_up_date मध्ये टाकते.
    @Modifying
    @Query(value = """
        UPDATE patients p 
        SET p.follow_up_date = (
            SELECT MIN(v.follow_up_date) 
            FROM visits v 
            WHERE v.patient_id = p.id 
            AND v.follow_up_date >= :today
        ) 
        WHERE p.id = :patientId
    """, nativeQuery = true)
    void autoSyncFollowUpDate(@Param("patientId") Long patientId, @Param("today") LocalDate today);}

