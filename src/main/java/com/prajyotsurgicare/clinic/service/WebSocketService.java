package com.prajyotsurgicare.clinic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // 📢 डॉक्टरांना Alert पाठवणारी मेथड
    public void sendQueueUpdate(Long clinicId, String type) {
        // Destination: /topic/clinic/{id}
        String destination = "/topic/clinic/" + clinicId;

        // Message Type: "REFRESH", "EMERGENCY", "BILLING_DONE"
        // Frontend ला कळेल की नक्की काय झाले आहे
        messagingTemplate.convertAndSend(destination, type);

        System.out.println("📢 WebSocket Update Sent to Clinic " + clinicId + ": " + type);
    }
}