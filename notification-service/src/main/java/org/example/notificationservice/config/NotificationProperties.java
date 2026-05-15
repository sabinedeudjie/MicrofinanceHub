package org.example.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
@Getter
@Setter
public class NotificationProperties {
    private String fromEmail  = "aa61af001@smtp-brevo.com";
    private String fromName   = "MicroFinanceHub";
    private String smsFrom    = "+237000000000";
    private int    retryMax   = 3;
}
