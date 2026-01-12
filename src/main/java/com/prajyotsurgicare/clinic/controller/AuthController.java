package com.prajyotsurgicare.clinic.controller;

import com.prajyotsurgicare.clinic.dto.AuthRequest;
import com.prajyotsurgicare.clinic.dto.AuthResponse;
import com.prajyotsurgicare.clinic.entity.Role;
import com.prajyotsurgicare.clinic.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService service;

    // 🔐 Login API
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    // 🆕 Register API (फक्त टेस्टिंगसाठी किंवा ॲडमिनसाठी)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(service.register(request, Role.RECEPTIONIST)); // Role हार्डकोड किंवा पॅरामीटरने घ्या
    }
}