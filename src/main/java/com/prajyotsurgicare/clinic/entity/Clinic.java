package com.prajyotsurgicare.clinic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ✅ हे ॲड करा (Auto Increment)
    private Long id;

    private String name;
    private String address;

    // Constructor for Data Seeder (Optional)
    public Clinic(String name, String address) {
        this.name = name;
        this.address = address;
    }
}