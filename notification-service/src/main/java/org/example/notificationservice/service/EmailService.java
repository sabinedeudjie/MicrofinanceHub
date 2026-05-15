package org.example.notificationservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.config.NotificationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender        mailSender;
    private final TemplateEngine        templateEngine;
    private final NotificationProperties properties;

    // 
    //  TEXTE BRUT
    // 
    public void envoyer(String destinataire, String sujet, String message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(properties.getFromEmail(), properties.getFromName());
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(message, false);
            mailSender.send(mime);
            log.info("Envoyé à {} | Sujet: {}", destinataire, sujet);
        } catch (Exception e) {
            log.error("Échec envoi à {} : {}", destinataire, e.getMessage());
            throw new RuntimeException("Échec envoi email : " + e.getMessage());
        }
    }

    // 
    //  HTML avec template Thymeleaf
    // 
    public void envoyerHtml(String destinataire, String sujet,
                            String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(properties.getFromEmail(), properties.getFromName());
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(htmlContent, true);
            mailSender.send(mime);
            log.info("-HTML] Envoyé à {} | Template: {}", destinataire, templateName);
        } catch (Exception e) {
            log.error("-HTML] Échec envoi à {} : {}", destinataire, e.getMessage());
            throw new RuntimeException("Échec envoi email HTML : " + e.getMessage());
        }
    }

    // 
    //  MÉTIER
    // 

    public void envoyerPretApprouve(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Votre prêt a été approuvé - MicroFinanceHub",
                "prêt-approuvé", vars);
    }

    public void envoyerPretRejete(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Votre demande de pret n'a pas ete acceptee - MicroFinanceHub",
                "pret-rejete", vars);
    }

    public void envoyerPretDecaisse(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Votre pret a ete decaisse - MicroFinanceHub",
                "pret-decaisse", vars);
    }

    public void envoyerRappelEcheance(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Rappel echeance - MicroFinanceHub",
                "rappel-echeance", vars);
    }

    public void envoyerConfirmationRemboursement(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Remboursement confirme - MicroFinanceHub",
                "confirmation-remboursement", vars);
    }

    public void envoyerAlertRetard(String dest, Map<String, Object> vars) {
        envoyerHtml(dest, "Alerte retard de paiement - MicroFinanceHub",
                "alerte-retard", vars);
    }
}
