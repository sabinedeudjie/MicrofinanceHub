package org.example.authservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.example.commonservice.event.UserLoginEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLoginEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.user-login}")
    private String userLoginRoutingKey;

    /**
     * Publie un événement de login utilisateur
     */
    public void publishUserLogin(String email, String userId, String ipAddress, String userAgent, String sessionId) {
        try {
            UserLoginEvent event = UserLoginEvent.builder()
                    .email(email)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .sessionId(sessionId)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(userEventsExchange, userLoginRoutingKey, event);
            log.info("de login publié pour: {}", email);
            
        } catch (Exception e) {
            log.error("lors de la publication de l'événement pour {}: {}", email, e.getMessage());
            //  pas bloquer le login si l'événement échoue
        }
    }
}