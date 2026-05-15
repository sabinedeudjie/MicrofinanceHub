package org.example.authservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Nécessite une infrastructure complète (PostgreSQL, Redis, RabbitMQ, Eureka)")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
