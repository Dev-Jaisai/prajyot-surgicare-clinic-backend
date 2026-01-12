package com.prajyotsurgicare.clinic.service;

import com.prajyotsurgicare.clinic.dto.AuthRequest;
import com.prajyotsurgicare.clinic.dto.AuthResponse;
import com.prajyotsurgicare.clinic.entity.Role;
import com.prajyotsurgicare.clinic.entity.User;
import com.prajyotsurgicare.clinic.repository.UserRepository;
import com.prajyotsurgicare.clinic.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ✅ 1. LOGIN METHOD (Updated with IDs)
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);

        // IDs आणि नाव चेक करून पाठवा
        Long doctorId = (user.getDoctor() != null) ? user.getDoctor().getId() : 0L;
        Long clinicId = (user.getClinic() != null) ? user.getClinic().getId() : 0L;
        String name = (user.getName() != null) ? user.getName() : user.getUsername();

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .name(name)
                .doctorId(doctorId)
                .clinicId(clinicId)
                .build();
    }

    // ✅ 2. REGISTER METHOD (Missing in your code, added back)
    public AuthResponse register(AuthRequest request, Role role) {
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .name(request.getUsername()) // Default name set to username
                .build();
        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole().name())
                .name(user.getUsername())
                .doctorId(0L)
                .clinicId(0L)
                .build();
    }
}