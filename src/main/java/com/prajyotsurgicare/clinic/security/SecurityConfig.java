package com.prajyotsurgicare.clinic.security;

import com.prajyotsurgicare.clinic.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF बंद
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    // ✅ CORS Master Fix
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        // ✅ 1. Public Endpoints (लॉगिन न करता ॲक्सेस)
                        .requestMatchers(
                                "/api/auth/**",
                                "/ws/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        // ✅ Prescription Endpoints - Shared Access
                        .requestMatchers("/api/prescription/**")
                        .hasAnyRole("RECEPTIONIST", "DOCTOR")
                        // ✅ 2. Shared Access (रिसेप्शनिस्ट आणि डॉक्टर दोघांनाही परवानगी)
                        // डॉक्टरांना पेशंट ॲड करण्यासाठी आणि डॅशबोर्ड बघण्यासाठी हे गरजेचे आहे.
                        .requestMatchers(
                                "/api/patients/**",
                                "/api/visits/**",
                                "/api/dashboard/**",
                                "/api/visit-types/**"// 👈 हे मिसिंग असू शकते!
                        ).hasAnyRole("RECEPTIONIST", "DOCTOR")

                        // ✅ 3. Doctor Specific (फक्त डॉक्टरांसाठी)
                        .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR")

                        // 🔒 4. बाकी सर्व रिक्वेस्टना लॉगिन अनिवार्य
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}