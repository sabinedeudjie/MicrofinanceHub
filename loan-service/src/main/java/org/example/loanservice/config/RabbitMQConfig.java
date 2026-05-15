package org.example.loanservice.config;

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
    
    //  des exchanges et queues
    public static final String LOAN_EXCHANGE = "loan.exchange";
    public static final String LOAN_APPLIED_QUEUE = "loan.applied.queue";
    public static final String LOAN_APPROVED_QUEUE = "loan.approved.queue";
    public static final String LOAN_DISBURSED_QUEUE = "loan.disbursed.queue";
    public static final String LOAN_REJECTED_QUEUE = "loan.rejected.queue";

    //  keys
    public static final String LOAN_APPLIED_KEY = "loan.applied";
    public static final String LOAN_APPROVED_KEY = "loan.approved";
    public static final String LOAN_DISBURSED_KEY = "loan.disbursed";
    public static final String LOAN_REJECTED_KEY = "loan.rejected";

    @Bean
    public TopicExchange loanExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }

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