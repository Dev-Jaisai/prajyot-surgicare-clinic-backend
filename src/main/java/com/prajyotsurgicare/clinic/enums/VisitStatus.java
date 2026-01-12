package com.prajyotsurgicare.clinic.enums;

public enum VisitStatus {

    BOOKED,        // Phone appointment – patient not yet arrived
    ARRIVED,       // Patient physically present
    IN_PROGRESS,   // Doctor consulting
    COMPLETED,     // Visit done
    CANCELLED,
    PENDING_BILLING,
    BILLING_PENDING, // डॉक्टरांनी तपासले, बिल बाकी// Appointment cancelled / no-show
}
