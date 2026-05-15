package org.example.authservice.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.mail.from:aa61af001@smtp-brevo.com}")
    private String fromAddress;

    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Réinitialisation de votre mot de passe - MicroFinanceHub";
        String html = buildResetEmailHtml(resetLink);
        sendHtmlEmail(to, subject, html);
    }

    public void sendVerificationEmail(String to, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        String subject = "Vérifiez votre adresse email - MicroFinanceHub";
        String html = buildVerificationEmailHtml(verifyLink);
        sendHtmlEmail(to, subject, html);
    }

    public void sendWelcomeEmail(String to, String firstName) {
        String subject = "Bienvenue chez MicroFinanceHub !";
        String html = buildWelcomeEmailHtml(firstName);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email envoye a: {}", to);
        } catch (Exception e) {
            log.error("Echec envoi email a {}: {}", to, e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email : " + e.getMessage());
        }
    }

    private String buildResetEmailHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#fff;font-size:22px;font-weight:700;">MicroFinanceHub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="margin:0 0 16px;color:#1e293b;font-size:18px;">Réinitialisation du mot de passe</h2>
                        <p style="color:#64748b;font-size:14px;line-height:1.6;margin:0 0 24px;">
                          Vous avez demandé la réinitialisation de votre mot de passe.<br>
                          Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.
                        </p>
                        <div style="text-align:center;margin:32px 0;">
                          <a href="%s" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;">
                            Réinitialiser mon mot de passe
                          </a>
                        </div>
                        <p style="color:#94a3b8;font-size:12px;line-height:1.5;margin:0;">
                          Ce lien est valable pendant <strong>1 heure</strong>.<br>
                          Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                        <p style="margin:0;color:#94a3b8;font-size:12px;">MicroFinanceHub &mdash; Cameroun</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(resetLink);
    }

    private String buildVerificationEmailHtml(String verifyLink) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#fff;font-size:22px;">MicroFinanceHub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="margin:0 0 16px;color:#1e293b;">Vérification de votre email</h2>
                        <p style="color:#64748b;font-size:14px;line-height:1.6;margin:0 0 24px;">
                          Cliquez sur le bouton ci-dessous pour vérifier votre adresse email.
                        </p>
                        <div style="text-align:center;margin:32px 0;">
                          <a href="%s" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:15px;font-weight:600;">
                            Vérifier mon email
                          </a>
                        </div>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(verifyLink);
    }

    private String buildWelcomeEmailHtml(String firstName) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:linear-gradient(135deg,#1e3a8a,#2563eb);padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#fff;font-size:22px;">MicroFinanceHub</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="margin:0 0 16px;color:#1e293b;">Bienvenue, %s !</h2>
                        <p style="color:#64748b;font-size:14px;line-height:1.6;">
                          Votre compte MicroFinanceHub a été créé avec succès.
                          Vous pouvez maintenant accéder à tous nos services de microfinance.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(firstName);
    }
}
