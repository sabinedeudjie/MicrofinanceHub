package org.example.notificationservice.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.config.NotificationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.enabled:false}")
    private boolean twilioEnabled;

    private final NotificationProperties properties;

    public SmsService(NotificationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (twilioEnabled && !accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialisé (from={})", properties.getSmsFrom());
        } else {
            log.warn("Twilio désactivé — les SMS seront loggés uniquement. " +
                     "Renseigner twilio.account-sid, twilio.auth-token et twilio.enabled=true pour activer.");
        }
    }

    public void envoyer(String telephone, String message) {
        if (!twilioEnabled || accountSid.isBlank() || authToken.isBlank()) {
            log.info("-LOG] À: {} | Message: {}", telephone, message);
            return;
        }

        try {
            Message msg = Message.creator(
                    new PhoneNumber(telephone),
                    new PhoneNumber(properties.getSmsFrom()),
                    message
            ).create();

            log.info("Envoyé à {} | SID: {}", telephone, msg.getSid());
        } catch (Exception e) {
            log.error("Échec envoi à {} : {}", telephone, e.getMessage());
            throw new RuntimeException("Échec envoi SMS : " + e.getMessage());
        }
    }
}
