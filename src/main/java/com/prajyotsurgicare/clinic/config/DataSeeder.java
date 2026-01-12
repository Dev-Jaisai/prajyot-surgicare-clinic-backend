package com.prajyotsurgicare.clinic.config;

import com.prajyotsurgicare.clinic.entity.Clinic;
import com.prajyotsurgicare.clinic.entity.Doctor;
import com.prajyotsurgicare.clinic.entity.Role;
import com.prajyotsurgicare.clinic.entity.User;
import com.prajyotsurgicare.clinic.repository.ClinicRepository;
import com.prajyotsurgicare.clinic.repository.DoctorRepository;
import com.prajyotsurgicare.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("🌱 Seeding Data...");

        // 1. Create Clinics (Check by Name to avoid duplicates)
        Clinic wakad = createClinicIfNotFound("Wakad", "Wakad, Pune");
        Clinic mahalunge = createClinicIfNotFound("Mahalunge", "Mahalunge, Pune");

        // 2. Create Doctors
        Doctor docNikhil = createDoctorIfNotFound("Dr. Nikhil (Ortho)");
        Doctor docPriyanka = createDoctorIfNotFound("Dr. Priyanka (ENT)");

        // 3. Create Users

        // A. Receptionist Roshani
        createUserIfNotFound("roshani", "w123", Role.RECEPTIONIST, wakad, null, "Roshani");

        // B. Receptionist Pooja
        createUserIfNotFound("pooja", "m123", Role.RECEPTIONIST, mahalunge, null, "Pooja");

        // C. Dr. Nikhil (User)
        createUserIfNotFound("nikhil", "doc123", Role.DOCTOR, wakad, docNikhil, "Dr. Nikhil");

        // D. Dr. Priyanka (User)
        createUserIfNotFound("priyanka", "doc123", Role.DOCTOR, mahalunge, docPriyanka, "Dr. Priyanka");

        System.out.println("✅ Data Seeding Completed!");
    }

    // --- Helper Methods ---

    private Clinic createClinicIfNotFound(String name, String address) {
        // ID 1, 2 शोधण्यापेक्षा नावाने शोधणे Safe आहे
        return clinicRepository.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Clinic c = new Clinic();
                    c.setName(name);
                    c.setAddress(address);
                    return clinicRepository.save(c);
                });
    }

    private Doctor createDoctorIfNotFound(String name) {
        return doctorRepository.findAll().stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Doctor d = new Doctor();
                    d.setName(name);
                    return doctorRepository.save(d);
                });
    }

    private void createUserIfNotFound(String username, String password, Role role, Clinic clinic, Doctor doctor, String name) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .clinic(clinic)
                    .doctor(doctor)
                    .name(name)
                    .build();
            userRepository.save(user);
            System.out.println("👉 User created: " + username);
        }
    }
}