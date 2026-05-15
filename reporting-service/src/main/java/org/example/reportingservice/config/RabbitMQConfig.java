package org.example.reportingservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    //  sources (publiés par loan-service et repayment-service)
    public static final String LOAN_EXCHANGE      = "loan.exchange";
    public static final String REPAYMENT_EXCHANGE = "repayment.exchange";

    //  dédiées au reporting (noms distincts pour ne pas concurrencer les autres consumers)
    public static final String REPORTING_LOAN_QUEUE      = "reporting.loan.queue";
    public static final String REPORTING_REPAYMENT_QUEUE = "reporting.repayment.queue";

    @Bean
    public TopicExchange loanExchange() {
        return ExchangeBuilder.topicExchange(LOAN_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange repaymentExchange() {
        return ExchangeBuilder.topicExchange(REPAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue loanQueue() {
        return QueueBuilder.durable(REPORTING_LOAN_QUEUE)
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Queue repaymentQueue() {
        return QueueBuilder.durable(REPORTING_REPAYMENT_QUEUE)
                .withArgument("x-max-length", 10000)
                .build();
    }

    @Bean
    public Binding loanBinding() {
        return BindingBuilder.bind(loanQueue()).to(loanExchange()).with("loan.*");
    }

    @Bean
    public Binding repaymentBinding() {
        return BindingBuilder.bind(repaymentQueue()).to(repaymentExchange()).with("payment.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
