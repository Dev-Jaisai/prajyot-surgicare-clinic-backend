package com.prajyotsurgicare.clinic.repository;

import com.prajyotsurgicare.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
}
