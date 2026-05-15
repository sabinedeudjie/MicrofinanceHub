package org.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {


    public static final String LOAN_EXCHANGE      = "loan.exchange";
    public static final String REPAYMENT_EXCHANGE = "repayment.exchange";
    public static final String ACCOUNT_EXCHANGE   = "compte.exchange";
    public static final String CLIENT_EXCHANGE    = "client.exchange";


    public static final String QUEUE_CLIENT_BIENVENUE = "notif.client.bienvenue.queue";
    public static final String QUEUE_PRET_APPROUVE   = "notif.pret.approuve.queue";
    public static final String QUEUE_PRET_REJETE     = "notif.pret.rejete.queue";
    public static final String QUEUE_PRET_DECAISSE   = "notif.pret.decaisse.queue";
    public static final String QUEUE_PAYMENT_RECU    = "notif.payment.recu.queue";
    public static final String QUEUE_PAYMENT_RETARD  = "notif.payment.retard.queue";
    public static final String QUEUE_COMPTE          = "notif.compte.queue";
    public static final String QUEUE_ALERTE_SOLDE   = "notif.compte.alerte.queue";


    @Bean
    public TopicExchange loanExchange() {
        return ExchangeBuilder.topicExchange(LOAN_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange repaymentExchange() {
        return ExchangeBuilder.topicExchange(REPAYMENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange accountExchange() {
        return ExchangeBuilder.topicExchange(ACCOUNT_EXCHANGE).durable(true).build();
    }

    //  QUEUES =====

    @Bean public Queue queueClientBienvenue() { return QueueBuilder.durable(QUEUE_CLIENT_BIENVENUE).build(); }
    @Bean public Queue queuePretApprouve()  { return QueueBuilder.durable(QUEUE_PRET_APPROUVE).build(); }
    @Bean public Queue queuePretRejete()    { return QueueBuilder.durable(QUEUE_PRET_REJETE).build(); }
    @Bean public Queue queuePretDecaisse()  { return QueueBuilder.durable(QUEUE_PRET_DECAISSE).build(); }
    @Bean public Queue queuePaymentRecu()   { return QueueBuilder.durable(QUEUE_PAYMENT_RECU).build(); }
    @Bean public Queue queuePaymentRetard() { return QueueBuilder.durable(QUEUE_PAYMENT_RETARD).build(); }
    @Bean public Queue queueCompte()        { return QueueBuilder.durable(QUEUE_COMPTE).build(); }
    @Bean public Queue queueAlerteSolde()   { return QueueBuilder.durable(QUEUE_ALERTE_SOLDE).build(); }

    //  EXCHANGE client =====

    @Bean
    public TopicExchange clientExchange() {
        return ExchangeBuilder.topicExchange(CLIENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding bindingClientBienvenue() {
        return BindingBuilder.bind(queueClientBienvenue()).to(clientExchange()).with("client.created");
    }

    //  BINDINGS — loan.exchange → queues prêt =====

    @Bean
    public Binding bindingPretApprouve() {
        return BindingBuilder.bind(queuePretApprouve()).to(loanExchange()).with("loan.approved");
    }

    @Bean
    public Binding bindingPretRejete() {
        return BindingBuilder.bind(queuePretRejete()).to(loanExchange()).with("loan.rejected");
    }

    @Bean
    public Binding bindingPretDecaisse() {
        return BindingBuilder.bind(queuePretDecaisse()).to(loanExchange()).with("loan.disbursed");
    }

    //  BINDINGS — repayment.exchange → queues paiement =====

    @Bean
    public Binding bindingPaymentRecu() {
        return BindingBuilder.bind(queuePaymentRecu()).to(repaymentExchange()).with("payment.received");
    }

    @Bean
    public Binding bindingPaymentRetard() {
        return BindingBuilder.bind(queuePaymentRetard()).to(repaymentExchange()).with("payment.overdue");
    }

    //  BINDINGS — account.exchange → queue compte =====

    @Bean
    public Binding bindingCompte() {
        return BindingBuilder.bind(queueCompte()).to(accountExchange()).with("compte.#");
    }

    @Bean
    public Binding bindingAlerteSolde() {
        return BindingBuilder.bind(queueAlerteSolde()).to(accountExchange()).with("compte.alerte.solde");
    }

    //  CONVERTISSEUR JSON =====

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
