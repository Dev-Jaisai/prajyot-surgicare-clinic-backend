package com.prajyotsurgicare.clinic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.prajyotsurgicare.clinic.enums.Gender;
import com.prajyotsurgicare.clinic.enums.VisitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PatientRegistrationRequest {

    // ✅ NEW FIELD: Existing Patient साठी ID गरजेचा आहे
    private Long patientId;

    // ✅ NEW FIELD: Multi-clinic सपोर्टसाठी
    private Long clinicId;

    // ---------------- PATIENT DETAILS ----------------
    // (नवीन पेशंट असेल तर हे लागतील, जुना असेल तर Frontend हे प्री-फिल करून पाठवेल)
    @NotBlank(message = "Patient name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits")
    private String mobile;

    private Gender gender;
    private Integer age;
    private String address;

    // ---------------- VISIT DETAILS ----------------
    @NotNull(message = "Visit type is required")
    private VisitType visitType;

    private String reason;

    @NotNull(message = "Doctor is required")
    private Long doctorId;

    // ---------------- FLOW CONTROL ----------------
    @JsonProperty("isAppointment")
    private boolean appointment;

    @JsonProperty("isEmergency")
    private boolean emergency;

    // ✅ Appointment Date
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @JsonProperty("createVisit")
    private boolean createVisit = true;

    double otherCharges = 0.0;
}