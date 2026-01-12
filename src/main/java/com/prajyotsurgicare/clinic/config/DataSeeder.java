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

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository; // ✅ Doctor Repo ॲड करा
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. Create Clinics (जर नसतील तर)
        Clinic wakad = clinicRepository.findById(1L).orElseGet(() -> {
            Clinic c = new Clinic();
            c.setName("Wakad");
            c.setAddress("Wakad, Pune");
            return clinicRepository.save(c);
        });

        Clinic mahalunge = clinicRepository.findById(2L).orElseGet(() -> {
            Clinic c = new Clinic();
            c.setName("Mahalunge");
            c.setAddress("Mahalunge, Pune");
            return clinicRepository.save(c);
        });

        // 2. Create Doctors (जर नसतील तर) & Save them FIRST!
        Doctor docNikhil = doctorRepository.findById(1L).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setName("Dr. Nikhil (Ortho)");
            return doctorRepository.save(d); // ✅ Save Doctor
        });

        Doctor docPriyanka = doctorRepository.findById(2L).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setName("Dr. Priyanka (ENT)");
            return doctorRepository.save(d); // ✅ Save Doctor
        });

        // 3. Create Users & LINK THEM

        // A. Receptionist Roshani (Wakad)
        if (userRepository.findByUsername("roshani").isEmpty()) {
            User user = User.builder()
                    .username("roshani")
                    .password(passwordEncoder.encode("w123"))
                    .role(Role.RECEPTIONIST)
                    .clinic(wakad) // ✅ Linked to Wakad
                    .build();
            userRepository.save(user);
            System.out.println("✅ User Created: roshani (Wakad)");
        }

        // B. Receptionist Pooja (Mahalunge)
        if (userRepository.findByUsername("pooja").isEmpty()) {
            User user = User.builder()
                    .username("pooja")
                    .password(passwordEncoder.encode("m123"))
                    .role(Role.RECEPTIONIST)
                    .clinic(mahalunge) // ✅ Linked to Mahalunge
                    .build();
            userRepository.save(user);
            System.out.println("✅ User Created: pooja (Mahalunge)");
        }

        // C. Dr. Nikhil (User linked to Doctor Entity)
        if (userRepository.findByUsername("nikhil").isEmpty()) {
            User user = User.builder()
                    .username("nikhil")
                    .password(passwordEncoder.encode("doc123"))
                    .role(Role.DOCTOR)
                    .doctor(docNikhil) // 🔥 IMP: Doctor ID Link केला
                    .name("Dr. Nikhil")
                    .build();
            userRepository.save(user);
            System.out.println("✅ User Created: Dr. Nikhil (Linked to Doc ID 1)");
        }

        // D. Dr. Priyanka (User linked to Doctor Entity)
        if (userRepository.findByUsername("priyanka").isEmpty()) {
            User user = User.builder()
                    .username("priyanka")
                    .password(passwordEncoder.encode("doc123"))
                    .role(Role.DOCTOR)
                    .doctor(docPriyanka) // 🔥 IMP: Doctor ID Link केला
                    .name("Dr. Priyanka")
                    .build();
            userRepository.save(user);
            System.out.println("✅ User Created: Dr. Priyanka (Linked to Doc ID 2)");
        }
    }
}