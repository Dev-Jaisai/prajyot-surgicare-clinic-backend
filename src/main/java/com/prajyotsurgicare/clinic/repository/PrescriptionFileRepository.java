package com.prajyotsurgicare.clinic.repository;

import com.prajyotsurgicare.clinic.entity.PrescriptionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionFileRepository extends JpaRepository<PrescriptionFile, Long> {

    // जुनी मेथड
    List<PrescriptionFile> findByVisitId(Long visitId);
    // ✅ Multiple Files साठी
    List<PrescriptionFile> findAllByVisitId(Long visitId);

    // ✅ 🔥 FIXED QUERY (Changed 'patientId' to 'id')
    @Query("SELECT pf FROM PrescriptionFile pf " +
            "JOIN pf.visit v " +
            "WHERE v.patient.id = :patientId " +  // 👈 HERE WAS THE ERROR
            "ORDER BY pf.id DESC") // Or uploadedAt if available
    List<PrescriptionFile> findRecentByPatientId(@Param("patientId") Long patientId);
}