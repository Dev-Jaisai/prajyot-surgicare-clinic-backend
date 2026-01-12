package com.prajyotsurgicare.clinic.entity;

import com.prajyotsurgicare.clinic.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients") // ✅ फक्त एवढेच ठेवा
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private Integer age;

    private String address;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
}
