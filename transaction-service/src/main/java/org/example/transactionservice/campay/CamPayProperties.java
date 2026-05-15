package org.example.transactionservice.campay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "campay")
@Data
public class CamPayProperties {

    private Base base = new Base();
    private App app = new App();

    @Data
    public static class Base {
        private String url;
    }

    @Data
    public static class App {
        private String username;
        private String password;
        private String permanentAccessToken;
        private Webhook webhook = new Webhook();

        @Data
        public static class Webhook {
            private String key;
        }
    }
}
