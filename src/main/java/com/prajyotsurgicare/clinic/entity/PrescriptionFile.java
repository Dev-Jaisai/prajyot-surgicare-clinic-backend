package com.prajyotsurgicare.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "prescription_files")
public class PrescriptionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Lob
    // ❌ OLD: @Column(columnDefinition = "LONGBLOB")  <- हे काढलं
    // ✅ NEW: फक्त @Lob ठेवा, Postgres आपोआप हँडल करेल
    private byte[] data;

    private String fileName;
    private String fileType;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}