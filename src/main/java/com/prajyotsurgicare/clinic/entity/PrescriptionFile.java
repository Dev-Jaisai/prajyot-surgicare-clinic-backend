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

    // ❌ OLD (Causing Issue): @OneToOne
    // @OneToOne
    // @JoinColumn(name = "visit_id", unique = true)

    // ✅ NEW (FIX): @ManyToOne (Remove unique=true)
    @ManyToOne
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;

    private String fileName;
    private String fileType;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}