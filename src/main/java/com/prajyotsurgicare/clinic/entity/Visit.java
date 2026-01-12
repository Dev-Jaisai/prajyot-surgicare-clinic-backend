package com.prajyotsurgicare.clinic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.prajyotsurgicare.clinic.enums.VisitStatus;
import com.prajyotsurgicare.clinic.enums.VisitType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    private VisitType visitType;

    private String reason;

    @Column(length = 1000)
    private String diagnosis;

    private LocalDate visitDate;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private VisitStatus status;

    // 💰 Financials
    private Double consultationFee;
    private Double otherCharges;
    private Double totalAmount;
    private Double paidAmount;

    @Column(columnDefinition = "TEXT")
    private String procedures;

    private String paymentMode;

    private Integer queueOrder;

    // ❌ OLD: columnDefinition = "TINYINT(1)..."
    // ✅ NEW: Postgres uses native BOOLEAN
    @Column(name = "is_emergency", nullable = false)
    private boolean emergency = false;

    @Column(name = "payment_collected")
    private boolean paymentCollected = false;

    private Integer tokenNumber;

    // Vitals
    private String bp;
    private String temperature;
    private String pulse;
    private String weight;

    @Column(columnDefinition = "TEXT")
    private String prescriptionNote;

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PrescriptionFile> prescriptionFiles = new ArrayList<>();

    @Column(name = "completion_date_time")
    private LocalDateTime completionDateTime;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
}