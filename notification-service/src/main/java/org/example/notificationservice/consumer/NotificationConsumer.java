package org.example.notificationservice.consumer;

import org.example.notificationservice.config.RabbitMQConfig;
import org.example.notificationservice.model.dto.event.ClientEvent;
import org.example.notificationservice.model.dto.event.CompteEvent;
import org.example.notificationservice.model.dto.event.PretEvent;
import org.example.notificationservice.model.dto.event.RemboursementEvent;
import org.example.notificationservice.model.dto.event.SoldeInsuffisantEvent;
import org.example.notificationservice.model.dto.NotificationRequest;
import org.example.notificationservice.model.enums.CanalNotification;
import org.example.notificationservice.model.enums.TypeNotification;
import org.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CLIENT_BIENVENUE)
    public void handleClientCree(ClientEvent event) {
        log.info("[CLIENT CRÉÉ] clientId={} email={}", event.getClientId(), event.getEmail());
        if (event.getEmail() == null) return;

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .email(event.getEmail())
                .telephone(event.getPhoneNumber())
                .sujet("Bienvenue chez MicroFinanceHub !")
                .message(String.format(
                    "Bonjour %s %s,\n\n" +
                    "Votre profil client a été enregistré par votre agence MicroFinanceHub.\n\n" +
                    "Vous pouvez maintenant créer votre accès en ligne en vous rendant sur notre plateforme " +
                    "et en cliquant sur \"Créer mon Compte\".\n\n" +
                    "Utilisez l'adresse email suivante pour vous inscrire : %s\n\n" +
                    "Cordialement,\nL'équipe MicroFinanceHub",
                    event.getFirstName(), event.getLastName(), event.getEmail()))
                .type(TypeNotification.CREATION_COMPTE)
                .canal(CanalNotification.EMAIL)
                .priorite(3)
                .referenceId(event.getClientId())
                .referenceType("CLIENT")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRET_APPROUVE)
    public void handlePretApprouve(PretEvent event) {
        log.info("[PRET APPROUVÉ] loanId={} client={}", event.getLoanId(), event.getClientEmail());
        if (event.getClientEmail() == null) return;

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .email(event.getClientEmail())
                .sujet("Votre prêt a été approuvé")
                .message(String.format(
                    "Bonjour %s,\n\nVotre demande de prêt a été approuvée.\n\n" +
                    "Montant : %.0f XAF\nDurée : %d mois\nMensualité : %.0f XAF\n\nMicroFinanceHub",
                    event.getClientNom(), event.getAmount(), event.getTermMonths(), event.getMonthlyPayment()))
                .type(TypeNotification.APPROBATION_PRET)
                .canal(CanalNotification.EMAIL)
                .priorite(2)
                .referenceId(event.getLoanId())
                .referenceType("PRET")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRET_REJETE)
    public void handlePretRejete(PretEvent event) {
        log.info("[PRÊT REJETÉ] loanId={} client={}", event.getLoanId(), event.getClientEmail());
        if (event.getClientEmail() == null) return;

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .email(event.getClientEmail())
                .sujet("Votre demande de prêt a été rejetée")
                .message(String.format(
                    "Bonjour %s,\n\nVotre demande de prêt n'a pas été approuvée.\n\n" +
                    "Motif : %s\n\nMicroFinanceHub",
                    event.getClientNom(),
                    event.getReason() != null ? event.getReason() : "Dossier incomplet"))
                .type(TypeNotification.REJET_PRET)
                .canal(CanalNotification.EMAIL)
                .priorite(3)
                .referenceType("PRET")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRET_DECAISSE)
    public void handlePretDecaisse(PretEvent event) {
        log.info("[PRÊT DÉCAISSÉ] loanId={} client={}", event.getLoanId(), event.getClientEmail());
        if (event.getClientEmail() == null) return;

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .email(event.getClientEmail())
                .sujet("Votre prêt a été décaissé")
                .message(String.format(
                    "Bonjour %s,\n\nVotre prêt de %.0f XAF a été décaissé.\n\n" +
                    "Prochaine échéance : %s\n\nMicroFinanceHub",
                    event.getClientNom(), event.getAmount(), event.getNextPaymentDate()))
                .type(TypeNotification.DECAISSEMENT)
                .canal(CanalNotification.EMAIL)
                .priorite(1)
                .referenceType("PRET")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_RECU)
    public void handlePaymentRecu(RemboursementEvent event) {
        log.info("[PAIEMENT REÇU] loanId={} montant={}", event.getLoanId(), event.getAmount());

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .email(event.getClientEmail())
                .sujet("Remboursement confirmé — MicroFinanceHub")
                .message(String.format(
                    "Bonjour %s,\n\n" +
                    "Nous confirmons la réception de votre remboursement de %.0f XAF sur le prêt %s.\n" +
                    "Merci pour votre confiance.\n\nMicroFinanceHub",
                    event.getClientNom() != null ? event.getClientNom() : "cher client",
                    event.getAmount(), event.getLoanId()))
                .type(TypeNotification.CONFIRMATION_REMB)
                .canal(CanalNotification.EMAIL)
                .priorite(3)
                .referenceType("REMBOURSEMENT")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_RETARD)
    public void handlePaymentRetard(RemboursementEvent event) {
        log.info("[RETARD] loanId={} jours={}", event.getLoanId(), event.getDaysOverdue());

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(event.getClientId())
                .sujet("Alerte : retard de paiement")
                .message(String.format(
                    "Votre paiement est en retard de %d jour(s).\n" +
                    "Montant dû : %.0f XAF\n\n" +
                    "Régularisez rapidement pour éviter des pénalités.\n\nMicroFinanceHub",
                    event.getDaysOverdue(), event.getDueAmount()))
                .type(TypeNotification.ALERTE_RETARD)
                .canal(CanalNotification.EMAIL)
                .priorite(1)
                .referenceType("PRET")
                .build());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_COMPTE)
    public void handleCompteEvent(CompteEvent event) {
        log.info("[COMPTE] clientId={} type={}", event.getClientId(), event.getTypeEvent());
        if (event.getClientEmail() == null) return;

        String typeLabel = libelleTypeCompte(event.getTypeCompte());

        switch (event.getTypeEvent() != null ? event.getTypeEvent() : "") {

            case "OUVERT" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Votre compte " + typeLabel + " MicroFinanceHub a été créé")
                    .message(String.format(
                        "Bonjour %s,\n\n" +
                        "Votre compte %s a bien été créé par votre agence MicroFinanceHub.\n\n" +
                        "Numéro de compte : %s\n\n" +
                        "Ce compte est actuellement en attente de validation par votre directeur d'agence.\n" +
                        "Vous recevrez une confirmation dès qu'il sera activé.\n\n" +
                        "Conservez précieusement ce numéro de compte.\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? event.getClientNom() : "",
                        typeLabel,
                        event.getNumeroCompte()))
                    .type(TypeNotification.CREATION_COMPTE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(2)
                    .referenceId(event.getNumeroCompte())
                    .referenceType("COMPTE")
                    .build());

            case "VALIDE" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Votre compte " + typeLabel + " est maintenant actif !")
                    .message(String.format(
                        "Bonjour %s,\n\n" +
                        "Bonne nouvelle ! Votre compte %s MicroFinanceHub a été validé et est maintenant actif.\n\n" +
                        "Votre numéro de compte : %s\n\n" +
                        "Vous pouvez vous présenter au guichet de votre agence avec ce numéro de compte " +
                        "pour effectuer vos opérations.\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? event.getClientNom() : "",
                        typeLabel,
                        event.getNumeroCompte()))
                    .type(TypeNotification.CREATION_COMPTE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(1)
                    .referenceId(event.getNumeroCompte())
                    .referenceType("COMPTE")
                    .build());

            case "REJETE" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Demande de compte " + typeLabel + " — décision")
                    .message(String.format(
                        "Bonjour %s,\n\n" +
                        "Nous vous informons que votre demande d'ouverture de compte %s (n° %s) " +
                        "n'a pas pu être validée.\n\n" +
                        "Pour plus d'informations, veuillez contacter votre agence MicroFinanceHub.\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? event.getClientNom() : "",
                        typeLabel,
                        event.getNumeroCompte()))
                    .type(TypeNotification.CREATION_COMPTE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(2)
                    .referenceId(event.getNumeroCompte())
                    .referenceType("COMPTE")
                    .build());

            case "DEPOT" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Dépôt reçu — " + event.getNumeroCompte())
                    .message(String.format(
                        "Bonjour%s,\n\n" +
                        "Un dépôt de %.0f XAF a été effectué sur votre compte %s.\n" +
                        "Solde après opération : %.0f XAF\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? " " + event.getClientNom() : "",
                        event.getMontant(), event.getNumeroCompte(), event.getSoldeApres()))
                    .type(TypeNotification.DEPOT_EFFECTUE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(3)
                    .referenceType("COMPTE")
                    .build());

            case "RETRAIT" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Retrait effectué — " + event.getNumeroCompte())
                    .message(String.format(
                        "Bonjour%s,\n\n" +
                        "Un retrait de %.0f XAF a été effectué sur votre compte %s.\n" +
                        "Solde après opération : %.0f XAF\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? " " + event.getClientNom() : "",
                        event.getMontant(), event.getNumeroCompte(), event.getSoldeApres()))
                    .type(TypeNotification.DEPOT_EFFECTUE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(3)
                    .referenceType("COMPTE")
                    .build());

            case "VIREMENT_SORTANT" -> notificationService.envoyerNotification(NotificationRequest.builder()
                    .clientId(event.getClientId())
                    .email(event.getClientEmail())
                    .telephone(event.getClientTelephone())
                    .sujet("Virement envoyé — " + event.getNumeroCompte())
                    .message(String.format(
                        "Bonjour%s,\n\n" +
                        "Un virement de %.0f XAF a été envoyé depuis votre compte %s" +
                        (event.getCompteContrepartie() != null ? " vers le compte " + event.getCompteContrepartie() : "") + ".\n" +
                        "Solde après opération : %.0f XAF\n\n" +
                        "Cordialement,\nL'équipe MicroFinanceHub",
                        event.getClientNom() != null ? " " + event.getClientNom() : "",
                        event.getMontant(), event.getNumeroCompte(), event.getSoldeApres()))
                    .type(TypeNotification.DEPOT_EFFECTUE)
                    .canal(CanalNotification.EMAIL)
                    .priorite(3)
                    .referenceType("COMPTE")
                    .build());

            case "VIREMENT_ENTRANT" -> {
                String expediteur = event.getNomContrepartie() != null
                        ? "M./Mme " + event.getNomContrepartie()
                        : (event.getCompteContrepartie() != null ? "le compte " + event.getCompteContrepartie() : "un autre compte");
                notificationService.envoyerNotification(NotificationRequest.builder()
                        .clientId(event.getClientId())
                        .email(event.getClientEmail())
                        .telephone(event.getClientTelephone())
                        .sujet("Virement reçu — " + event.getNumeroCompte())
                        .message(String.format(
                            "Bonjour%s,\n\n" +
                            "Un virement de %.0f XAF a été effectué par %s sur votre compte %s (%s).\n" +
                            "Solde après opération : %.0f XAF\n\n" +
                            "Référence : %s\n\n" +
                            "Cordialement,\nL'équipe MicroFinanceHub",
                            event.getClientNom() != null ? " " + event.getClientNom() : "",
                            event.getMontant(), expediteur,
                            event.getNumeroCompte(), typeLabel,
                            event.getSoldeApres(),
                            event.getCompteContrepartie() != null ? event.getCompteContrepartie() : "—"))
                        .type(TypeNotification.DEPOT_EFFECTUE)
                        .canal(CanalNotification.EMAIL)
                        .priorite(2)
                        .referenceType("COMPTE")
                        .build());
            }

            default -> log.debug("[COMPTE] typeEvent inconnu '{}', aucune notification envoyée.", event.getTypeEvent());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ALERTE_SOLDE)
    public void handleAlerteSolde(SoldeInsuffisantEvent event) {
        log.info("[ALERTE SOLDE] clientId={} numero={}", event.getClientId(), event.getNumeroCompte());
        if (event.getClientEmail() == null) return;

        notificationService.envoyerNotification(NotificationRequest.builder()
                .clientId(String.valueOf(event.getClientId()))
                .email(event.getClientEmail())
                .sujet("Alerte solde insuffisant — MicroFinanceHub")
                .message(String.format(
                    "Bonjour %s,\n\n" +
                    "Une tentative d'opération a échoué sur votre compte %s en raison d'un solde insuffisant.\n" +
                    "Montant tenté : %.0f XAF\n" +
                    "Solde actuel : %.0f XAF\n\n" +
                    "Veuillez approvisionner votre compte pour vos prochaines transactions.\n\nMicroFinanceHub",
                    event.getClientNom() != null ? event.getClientNom() : "cher client",
                    event.getNumeroCompte(), event.getMontantDemande(), event.getSoldeActuel()))
                .type(TypeNotification.ALERTE_SOLDE)
                .canal(CanalNotification.EMAIL)
                .priorite(1)
                .referenceType("COMPTE")
                .build());
    }

    private String libelleTypeCompte(String typeCompte) {
        if (typeCompte == null) return "bancaire";
        return switch (typeCompte) {
            case "EPARGNE"       -> "Épargne";
            case "COURANT"       -> "Courant";
            case "MICRO_EPARGNE" -> "Micro-Épargne";
            case "DEPOT_A_TERME" -> "Dépôt à Terme";
            case "CREDIT"        -> "Crédit";
            default              -> typeCompte;
        };
    }
}
