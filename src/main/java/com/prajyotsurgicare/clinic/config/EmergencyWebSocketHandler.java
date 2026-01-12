package com.prajyotsurgicare.clinic.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// websocket/EmergencyWebSocketHandler.java
@Component
public class EmergencyWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> clinicSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long clinicId = getClinicId(session);
        clinicSessions
                .computeIfAbsent(clinicId, k -> new ArrayList<>())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long clinicId = getClinicId(session);
        clinicSessions.getOrDefault(clinicId, List.of()).remove(session);
    }

    public void sendEmergency(Long clinicId, Long visitId) {
        List<WebSocketSession> sessions = clinicSessions.get(clinicId);
        if (sessions == null) return;

        String payload = visitId.toString();

        for (WebSocketSession s : sessions) {
            try {
                s.sendMessage(new TextMessage(payload));
            } catch (Exception ignored) {}
        }
    }

    private Long getClinicId(WebSocketSession session) {
        String query = session.getUri().getQuery(); // clinicId=1
        return Long.parseLong(query.split("=")[1]);
    }
}
