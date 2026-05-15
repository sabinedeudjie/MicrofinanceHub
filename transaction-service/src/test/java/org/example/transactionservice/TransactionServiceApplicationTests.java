package org.example.transactionservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Nécessite une infrastructure complète (PostgreSQL, RabbitMQ, Eureka)")
class TransactionServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
