package org.example.accountservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COMPTE_EXCHANGE      = "compte.exchange";
    public static final String QUEUE_NOTIFICATIONS  = "compte.notifications";
    public static final String QUEUE_ANALYTICS      = "compte.analytics";

    public static final String RK_COMPTE_OUVERT  = "compte.ouvert";
    public static final String RK_COMPTE_VALIDE  = "compte.valide";
    public static final String RK_COMPTE_REJETE  = "compte.rejete";
    public static final String RK_COMPTE_BLOQUE  = "compte.alerte.bloque";

    @Bean
    public TopicExchange compteExchange() {
        return new TopicExchange(COMPTE_EXCHANGE, true, false);
    }

    @Bean
    public Queue queueNotifications() {
        return QueueBuilder.durable(QUEUE_NOTIFICATIONS).build();
    }

    @Bean
    public Queue queueAnalytics() {
        return QueueBuilder.durable(QUEUE_ANALYTICS).build();
    }

    @Bean
    public Binding bindingNotifications(Queue queueNotifications, TopicExchange compteExchange) {
        return BindingBuilder.bind(queueNotifications).to(compteExchange).with("compte.#");
    }

    @Bean
    public Binding bindingAnalytics(Queue queueAnalytics, TopicExchange compteExchange) {
        return BindingBuilder.bind(queueAnalytics).to(compteExchange).with("compte.transaction.*");
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
