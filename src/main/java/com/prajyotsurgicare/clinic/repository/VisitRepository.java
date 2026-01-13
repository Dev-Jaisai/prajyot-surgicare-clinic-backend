package com.prajyotsurgicare.clinic.repository;

import com.prajyotsurgicare.clinic.entity.Visit;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

// VisitRepository.java

    // 🔥 NEW: Doctor च्या आजच्या एकूण visits (All statuses)
    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate = :date " +
            "AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId")
    long countByVisitDateAndClinicIdAndDoctorId(
            @Param("date") LocalDate date,
            @Param("clinicId") Long clinicId,
            @Param("doctorId") Long doctorId
    );

    List<Visit> findByVisitDateAndClinicIdAndDoctorIdAndStatusInOrderByEmergencyDescTokenNumberAsc(
            LocalDate date,
            Long clinicId,
            Long doctorId,
            List<VisitStatus> statuses
    );

    List<Visit> findTop10ByPatientIdOrderByVisitDateDesc(Long patientId);
    // 🔹 Patient related
    long countByPatientId(Long patientId);

    List<Visit> findByPatientIdOrderByVisitDateDesc(Long patientId);

    void deleteByPatientId(Long patientId);

    // 🔹 Date based
    long countByVisitDate(LocalDate visitDate);

    List<Visit> findByVisitDateOrderByIdAsc(LocalDate date);

    List<Visit> findByVisitDateOrderByIdDesc(LocalDate date);

    // 🔹 CLINIC WISE (ACTIVE USE)
    long countByVisitDateAndClinicId(LocalDate date, Long clinicId);

    long countByStatusAndVisitDateAndClinicId(VisitStatus status, LocalDate date, Long clinicId);

    // ✅ For Booked Appointments
    List<Visit> findByVisitDateAndClinicIdAndStatusOrderByQueueOrderAsc(
            LocalDate date, Long clinicId, VisitStatus status);

    // ✅ CORRECT METHOD (Used by Doctor Service)
    // Field name is 'emergency', so we use 'OrderByEmergency'
    List<Visit> findByVisitDateAndClinicIdAndStatusOrderByEmergencyDescTokenNumberAsc(
            LocalDate date, Long clinicId, VisitStatus status);

    @Query("SELECT MAX(v.tokenNumber) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId")
    Integer findMaxTokenByDoctor(@Param("date") LocalDate date, @Param("clinicId") Long clinicId, @Param("doctorId") Long doctorId);

    @Query("SELECT MAX(v.tokenNumber) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId")
    Integer findMaxToken(@Param("date") LocalDate date, @Param("clinicId") Long clinicId);

    // 2. Patient History (For 30-day logic & History Chip)
    Optional<Visit> findTopByPatientIdOrderByVisitDateDesc(Long patientId);


    // ✅ NEW: एकापेक्षा जास्त स्टेटस शोधण्यासाठी (ARRIVED + BILLING_PENDING)
    List<Visit> findByVisitDateAndClinicIdAndStatusInOrderByEmergencyDescTokenNumberAsc(
            LocalDate date,
            Long clinicId,
            List<VisitStatus> statuses
    );

    List<Visit> findByPatientIdAndStatusOrderByVisitDateDesc(Long patientId, VisitStatus status);
// VisitRepository.java

    // स्टेटस फिल्टर न लावता चेक करा (फक्त टेस्टिंगसाठी)
    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId")
    long countAllTodayVisits(LocalDate date, Long clinicId);

    // --- Clinic Level Queries ---
    @Query("SELECT COALESCE(SUM(v.totalAmount), 0) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId AND v.status = 'COMPLETED'")
    Double getDailyCollection(LocalDate date, Long clinicId);

    long countByVisitDateAndClinicIdAndStatus(LocalDate date, Long clinicId, VisitStatus status);

    @Query("SELECT COALESCE(SUM(v.totalAmount), 0) FROM Visit v WHERE v.visitDate BETWEEN :start AND :end AND v.clinic.id = :clinicId AND v.status = 'COMPLETED'")
    Double getMonthlyCollection(LocalDate start, LocalDate end, Long clinicId);

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate BETWEEN :start AND :end AND v.clinic.id = :clinicId AND v.status = 'COMPLETED'")
    long getMonthlyVisits(LocalDate start, LocalDate end, Long clinicId);

    @Query("SELECT SUM(v.totalAmount) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId AND v.status = 'COMPLETED'")
    Double getDailyCollectionByDoctor(LocalDate date, Long clinicId, Long doctorId);

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate = :date AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId AND v.status = 'COMPLETED'")
    long countByVisitDateAndClinicIdAndDoctorIdAndStatus(LocalDate date, Long clinicId, Long doctorId, VisitStatus status);
    @Query("SELECT COALESCE(SUM(v.totalAmount), 0) FROM Visit v WHERE v.visitDate BETWEEN :start AND :end AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId AND v.status = 'COMPLETED'")
    Double getMonthlyCollectionByDoctor(LocalDate start, LocalDate end, Long clinicId, Long doctorId);

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.visitDate BETWEEN :start AND :end AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId AND v.status = 'COMPLETED'")
    long getMonthlyVisitsByDoctor(LocalDate start, LocalDate end, Long clinicId, Long doctorId);

    // 1. Clinic Wise List (Date Range)
    List<Visit> findByVisitDateBetweenAndClinicIdAndStatus(
            LocalDate start, LocalDate end, Long clinicId, VisitStatus status);

    // 2. Doctor Wise List (Date Range)
    List<Visit> findByVisitDateBetweenAndClinicIdAndDoctorIdAndStatus(
            LocalDate start, LocalDate end, Long clinicId, Long doctorId, VisitStatus status);

    List<Visit> findByVisitDateAndClinicIdAndStatusInOrderByEmergencyDescQueueOrderAsc(
            LocalDate date,
            Long clinicId,
            List<VisitStatus> statuses
    );

    List<Visit> findByVisitDateAndClinicIdAndStatusOrderByEmergencyDescQueueOrderAsc(
            LocalDate date, Long clinicId, VisitStatus status);

    // ✅ Booked साठी Token Number नुसार सॉर्ट करा (QueueOrder नको)
    List<Visit> findByVisitDateAndClinicIdAndStatusOrderByTokenNumberAsc(
            LocalDate date, Long clinicId, VisitStatus status);

    List<Visit> findByFollowUpDate(LocalDate date);


    List<Visit> findByPatientIdAndFollowUpDateGreaterThanEqualOrderByFollowUpDateAsc(Long patientId, LocalDate date);
    // 🔥 NEW: या पेशंटची 'आज किंवा त्यानंतरची' सर्वात पहिली तारीख आणा
    // VisitRepository.java मध्ये ही query add करा:

    @Query("SELECT MIN(v.followUpDate) FROM Visit v WHERE v.patient.id = :patientId AND v.followUpDate >= CURRENT_DATE")
    Optional<LocalDate> findNextFollowUpDate(@Param("patientId") Long patientId);


    @Query("SELECT MIN(v.followUpDate) FROM Visit v WHERE v.patient.id = :patientId AND v.followUpDate >= :today")
    Optional<LocalDate> findNextFollowUpDate(@Param("patientId") Long patientId, @Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE Visit v SET v.followUpDate = :date WHERE v.id = :id")
    void updateFollowUpDateDirectly(@Param("id") Long id, @Param("date") LocalDate date);

    Optional<Visit> findTopByPatientIdAndDoctorIdAndStatusOrderByVisitDateDesc(
            Long patientId, Long doctorId, VisitStatus status);

    // ✅ CORRECT (New - Solo Doctor Logic)
    @Query("SELECT v FROM Visit v WHERE v.patient.id = :patientId " +
            "AND v.status = 'COMPLETED' " +
            "ORDER BY v.visitDate DESC")
    List<Visit> findLastVisits(@Param("patientId") Long patientId); // 🔥 आता फक्त 1 Argument}

    @Query("SELECT v FROM Visit v WHERE v.followUpDate = :today AND v.clinic.id = :clinicId AND v.doctor.id = :doctorId")
    List<Visit> findTodayFollowUps(LocalDate today, Long clinicId, Long doctorId);
    // VisitRepository.java
    List<Visit> findByFollowUpDateAndClinicIdAndDoctorId(LocalDate date, Long clinicId, Long doctorId);}