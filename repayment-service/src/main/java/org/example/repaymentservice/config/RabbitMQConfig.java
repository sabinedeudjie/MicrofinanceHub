package org.example.repaymentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 
    //  SERVICE - Écoute des événements
    // 
    public static final String LOAN_EXCHANGE = "loan.exchange";
    
    //  pour écouter les événements du Loan Service
    public static final String LOAN_APPLIED_QUEUE = "loan.applied.queue";
    public static final String LOAN_APPROVED_QUEUE = "loan.approved.queue";
    public static final String LOAN_DISBURSED_QUEUE = "loan.disbursed.queue";
    public static final String LOAN_REJECTED_QUEUE = "loan.rejected.queue";
    
    //  keys pour Loan Service
    public static final String LOAN_APPLIED_KEY = "loan.applied";
    public static final String LOAN_APPROVED_KEY = "loan.approved";
    public static final String LOAN_DISBURSED_KEY = "loan.disbursed";
    public static final String LOAN_REJECTED_KEY = "loan.rejected";

    // 
    //  SERVICE - Publication des événements
    // 
    public static final String REPAYMENT_EXCHANGE = "repayment.exchange";
    
    //  pour les événements de remboursement
    public static final String PAYMENT_RECEIVED_QUEUE = "payment.received.queue";
    public static final String PAYMENT_OVERDUE_QUEUE = "payment.overdue.queue";
    public static final String SCHEDULE_UPDATED_QUEUE = "schedule.updated.queue";
    
    //  keys pour Repayment Service
    public static final String PAYMENT_RECEIVED_KEY = "payment.received";
    public static final String PAYMENT_OVERDUE_KEY = "payment.overdue";
    public static final String SCHEDULE_UPDATED_KEY = "schedule.updated";

    // Confirmation Mobile Money (publié par transaction-service)
    public static final String MOBILE_CONFIRMED_QUEUE = "repayment.mobile.confirmed.queue";
    public static final String MOBILE_CONFIRMED_KEY   = "repayment.mobile.confirmed";
    public static final String MOBILE_FAILED_QUEUE    = "repayment.mobile.failed.queue";
    public static final String MOBILE_FAILED_KEY      = "repayment.mobile.failed";

    // 
    // 
    // 
    
    @Bean
    public TopicExchange loanExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }
    
    @Bean
    public TopicExchange repaymentExchange() {
        return new TopicExchange(REPAYMENT_EXCHANGE);
    }

    //  pour Loan Service (écoute)
    @Bean
    public Queue loanAppliedQueue() {
        return new Queue(LOAN_APPLIED_QUEUE, true);
    }
    
    @Bean
    public Queue loanApprovedQueue() {
        return new Queue(LOAN_APPROVED_QUEUE, true);
    }
    
    @Bean
    public Queue loanDisbursedQueue() {
        return new Queue(LOAN_DISBURSED_QUEUE, true);
    }
    
    @Bean
    public Queue loanRejectedQueue() {
        return new Queue(LOAN_REJECTED_QUEUE, true);
    }
    
    //  pour Repayment Service (publication)
    @Bean
    public Queue paymentReceivedQueue() {
        return new Queue(PAYMENT_RECEIVED_QUEUE, true);
    }

    @Bean
    public Queue paymentOverdueQueue() {
        return new Queue(PAYMENT_OVERDUE_QUEUE, true);
    }

    @Bean
    public Queue scheduleUpdatedQueue() {
        return new Queue(SCHEDULE_UPDATED_QUEUE, true);
    }

    // Queues pour confirmation/échec Mobile Money (reçus depuis transaction-service)
    @Bean
    public Queue mobileConfirmedQueue() {
        return new Queue(MOBILE_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue mobileFailedQueue() {
        return new Queue(MOBILE_FAILED_QUEUE, true);
    }

    @Bean
    public Binding mobileConfirmedBinding() {
        return BindingBuilder.bind(mobileConfirmedQueue()).to(repaymentExchange()).with(MOBILE_CONFIRMED_KEY);
    }

    @Bean
    public Binding mobileFailedBinding() {
        return BindingBuilder.bind(mobileFailedQueue()).to(repaymentExchange()).with(MOBILE_FAILED_KEY);
    }

    //  pour Loan Service
    @Bean
    public Binding loanAppliedBinding() {
        return BindingBuilder
            .bind(loanAppliedQueue())
            .to(loanExchange())
            .with(LOAN_APPLIED_KEY);
    }
    
    @Bean
    public Binding loanApprovedBinding() {
        return BindingBuilder
            .bind(loanApprovedQueue())
            .to(loanExchange())
            .with(LOAN_APPROVED_KEY);
    }
    
    @Bean
    public Binding loanDisbursedBinding() {
        return BindingBuilder
            .bind(loanDisbursedQueue())
            .to(loanExchange())
            .with(LOAN_DISBURSED_KEY);
    }
    
    @Bean
    public Binding loanRejectedBinding() {
        return BindingBuilder
            .bind(loanRejectedQueue())
            .to(loanExchange())
            .with(LOAN_REJECTED_KEY);
    }
    
    //  pour Repayment Service
    @Bean
    public Binding paymentReceivedBinding() {
        return BindingBuilder
            .bind(paymentReceivedQueue())
            .to(repaymentExchange())
            .with(PAYMENT_RECEIVED_KEY);
    }
    
    @Bean
    public Binding paymentOverdueBinding() {
        return BindingBuilder
            .bind(paymentOverdueQueue())
            .to(repaymentExchange())
            .with(PAYMENT_OVERDUE_KEY);
    }
    
    @Bean
    public Binding scheduleUpdatedBinding() {
        return BindingBuilder
            .bind(scheduleUpdatedQueue())
            .to(repaymentExchange())
            .with(SCHEDULE_UPDATED_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}