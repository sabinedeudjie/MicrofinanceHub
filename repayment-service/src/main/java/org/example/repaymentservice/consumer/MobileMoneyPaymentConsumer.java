package org.example.repaymentservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.repaymentservice.client.LoanServiceClient;
import org.example.repaymentservice.config.RabbitMQConfig;
import org.example.repaymentservice.dto.response.AmortizationResponse;
import org.example.repaymentservice.model.Payment;
import org.example.repaymentservice.model.Repayment;
import org.example.repaymentservice.model.enums.PaymentStatus;
import org.example.repaymentservice.repository.PaymentRepository;
import org.example.repaymentservice.repository.RepaymentRepository;
import org.example.repaymentservice.repository.ScheduleRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobileMoneyPaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final RepaymentRepository repaymentRepository;
    private final ScheduleRepository scheduleRepository;
    private final LoanServiceClient loanServiceClient;
    private final org.example.repaymentservice.client.ClientServiceClient clientServiceClient;
    private final org.example.repaymentservice.events.RepaymentEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitMQConfig.MOBILE_CONFIRMED_QUEUE)
    @Transactional
    public void handleMobileMoneyConfirmed(Map<String, Object> event) {
        String referenceRepayment = (String) event.get("referenceRepayment");
        String loanId = (String) event.get("loanId");

        log.info("Mobile Money confirmé par CamPay: ref={}, loanId={}", referenceRepayment, loanId);

        Optional<Payment> paymentOpt = paymentRepository.findByPaymentNumber(referenceRepayment);
        if (paymentOpt.isEmpty()) {
            log.error("Payment introuvable pour reference: {}", referenceRepayment);
            return;
        }

        Payment payment = paymentOpt.get();

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            log.warn("Payment {} déjà traité (statut={}), ignoré", referenceRepayment, payment.getStatus());
            return;
        }

        try {
            // Récupérer l'amortissement depuis loan-service avec un token interne
            // Le token est stocké temporairement dans les notes du payment
            String authorization = extractAuthFromNotes(payment.getNotes());
            if (authorization == null) {
                log.error("Token introuvable dans les notes du payment {}", referenceRepayment);
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return;
            }

            AmortizationResponse amortization = loanServiceClient.getLoanAmortization(loanId, authorization);
            if (amortization == null || amortization.getEntries() == null) {
                log.error("Plan d'amortissement introuvable pour loanId={}", loanId);
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return;
            }

            // Rejouer la logique: quelles échéances couvre le montant payé ?
            BigDecimal remaining = payment.getAmount();
            List<Integer> paidInstallments = new ArrayList<>();

            for (AmortizationResponse.AmortizationEntry entry : amortization.getEntries()) {
                if (entry.isPaid() || remaining.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal due = entry.getDueAmount();
                if (remaining.compareTo(due) >= 0) {
                    remaining = remaining.subtract(due);
                    paidInstallments.add(entry.getInstallmentNumber());

                    String schedulePaymentId = UUID.randomUUID().toString();
                    try {
                        loanServiceClient.markScheduleAsPaid(loanId, entry.getInstallmentNumber(),
                                schedulePaymentId, authorization);
                    } catch (Exception e) {
                        log.error("Erreur markScheduleAsPaid échéance {}: {}", entry.getInstallmentNumber(), e.getMessage());
                    }

                    updateLocalSchedule(loanId, entry.getInstallmentNumber(), schedulePaymentId, payment.getId());
                    createRepaymentRecord(payment, loanId, payment.getClientId(), entry);
                }
            }

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setNotes(cleanAuthFromNotes(payment.getNotes()) + " | Mobile Money confirmé par CamPay");
            paymentRepository.save(payment);

            log.info("Payment {} finalisé: {} échéance(s) payée(s) pour loanId={}",
                    referenceRepayment, paidInstallments.size(), loanId);

            // Publier l'événement de paiement reçu pour notification
            try {
                String clientEmail = (String) event.get("clientEmail");
                String clientNom = (String) event.get("clientNom");
                
                // Si pas dans l'événement, on le récupère
                if (clientEmail == null || clientNom == null) {
                    try {
                        org.example.repaymentservice.dto.ClientInfo info = clientServiceClient.getClientInfo(payment.getClientId(), authorization);
                        if (info != null) {
                            clientEmail = info.getEmail();
                            clientNom = info.getFirstName() + " " + info.getLastName();
                        }
                    } catch (Exception ce) {
                        log.warn("Impossible de récupérer les infos client pour notification Mobile Money: {}", ce.getMessage());
                    }
                }
                
                List<org.example.repaymentservice.model.Schedule> schedules = new ArrayList<>();
                for (Integer instNum : paidInstallments) {
                    scheduleRepository.findByLoanIdAndInstallmentNumber(loanId, instNum).ifPresent(schedules::add);
                }
                
                eventPublisher.publishPaymentReceived(payment, schedules, clientEmail, clientNom);
            } catch (Exception ee) {
                log.warn("Erreur lors de la publication de l'événement PaymentReceived (Mobile Money): {}", ee.getMessage());
            }

        } catch (Exception e) {
            log.error("Erreur finalisation payment Mobile Money {}: {}", referenceRepayment, e.getMessage(), e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.MOBILE_FAILED_QUEUE)
    @Transactional
    public void handleMobileMoneyFailed(Map<String, Object> event) {
        String referenceRepayment = (String) event.get("referenceRepayment");
        log.warn("Mobile Money échoué (CamPay): ref={}", referenceRepayment);

        paymentRepository.findByPaymentNumber(referenceRepayment).ifPresent(payment -> {
            if (PaymentStatus.PENDING.equals(payment.getStatus())) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setNotes(cleanAuthFromNotes(payment.getNotes()) + " | Mobile Money refusé par CamPay");
                paymentRepository.save(payment);
                log.info("Payment {} marqué FAILED", referenceRepayment);
            }
        });
    }

    private void updateLocalSchedule(String loanId, Integer installmentNumber, String paymentId, String paymentEntityId) {
        scheduleRepository.findByLoanIdAndInstallmentNumber(loanId, installmentNumber).ifPresent(s -> {
            s.setPaid(true);
            s.setPaidDate(LocalDateTime.now());
            s.setPaymentId(paymentId);
            scheduleRepository.save(s);
        });
    }

    private void createRepaymentRecord(Payment payment, String loanId, String clientId,
                                        AmortizationResponse.AmortizationEntry entry) {
        repaymentRepository.findByLoanIdAndInstallmentNumber(loanId, entry.getInstallmentNumber())
                .ifPresentOrElse(
                        rep -> {
                            rep.setPaidAmount(entry.getDueAmount());
                            rep.setStatus(PaymentStatus.COMPLETED);
                            rep.setPaidDate(LocalDateTime.now());
                            rep.setPaymentId(payment.getId());
                            repaymentRepository.save(rep);
                        },
                        () -> {
                            Repayment r = Repayment.builder()
                                    .id(UUID.randomUUID().toString())
                                    .paymentId(payment.getId())
                                    .loanId(loanId)
                                    .clientId(clientId)
                                    .installmentNumber(entry.getInstallmentNumber())
                                    .dueAmount(entry.getDueAmount())
                                    .paidAmount(entry.getDueAmount())
                                    .penaltyAmount(BigDecimal.ZERO)
                                    .dueDate(entry.getDueDate())
                                    .paidDate(LocalDateTime.now())
                                    .status(PaymentStatus.COMPLETED)
                                    .build();
                            repaymentRepository.save(r);
                        }
                );
    }

    // Le token JWT est stocké temporairement dans les notes avec le préfixe "__AUTH__:"
    private String extractAuthFromNotes(String notes) {
        if (notes == null) return null;
        for (String part : notes.split("\\|")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("__AUTH__:")) {
                return trimmed.substring("__AUTH__:".length()).trim();
            }
        }
        return null;
    }

    private String cleanAuthFromNotes(String notes) {
        if (notes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : notes.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.startsWith("__AUTH__:")) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(trimmed);
            }
        }
        return sb.toString();
    }
}
