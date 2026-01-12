package com.prajyotsurgicare.clinic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // ✅ हे Spring चे Magic Annotation आहे
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 📢 डॉक्टरांचे Frontend या prefix वर subscribe करेल (उदा. /topic/queue/1)
        config.enableSimpleBroker("/topic");

        // जर Frontend वरून काही डेटा पाठवायचा असेल तर तो '/app' ने सुरू होईल
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 🔌 हे कनेक्शन पॉईंट आहे. Angular इथून कनेक्ट करेल.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS Error टाळण्यासाठी
                .withSockJS(); // जर इंटरनेट स्लो असेल किंवा जुना ब्राउझर असेल तर हे मदत करते
    }
}