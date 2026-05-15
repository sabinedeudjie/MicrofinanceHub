package org.example.commonservice.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserLoginEvent — Tests unitaires")
class UserLoginEventTest {

    @Test
    @DisplayName("Construit un UserLoginEvent avec le builder")
    void shouldBuildUserLoginEvent() {
        LocalDateTime now = LocalDateTime.now();
        UserLoginEvent event = UserLoginEvent.builder()
            .email("agent@mfh.cm")
            .userId("user-uuid-001")
            .ipAddress("192.168.1.10")
            .userAgent("Mozilla/5.0")
            .sessionId("session-abc-123")
            .timestamp(now)
            .build();

        assertThat(event.getEmail()).isEqualTo("agent@mfh.cm");
        assertThat(event.getUserId()).isEqualTo("user-uuid-001");
        assertThat(event.getIpAddress()).isEqualTo("192.168.1.10");
        assertThat(event.getSessionId()).isEqualTo("session-abc-123");
        assertThat(event.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("Construit un JointAccountCreatedEvent avec le builder")
    void shouldBuildJointAccountCreatedEvent() {
        JointAccountCreatedEvent event = JointAccountCreatedEvent.builder()
            .jointAccountId("joint-001")
            .clientIds(java.util.List.of("client-a", "client-b"))
            .build();

        assertThat(event.getJointAccountId()).isEqualTo("joint-001");
        assertThat(event.getClientIds()).containsExactly("client-a", "client-b");
    }

    @Test
    @DisplayName("UserLoginEvent est sérialisable")
    void shouldBeSerializable() {
        UserLoginEvent event = UserLoginEvent.builder()
            .email("test@mfh.cm")
            .userId("uid-001")
            .timestamp(LocalDateTime.now())
            .build();

        assertThat(event).isInstanceOf(java.io.Serializable.class);
    }
}
