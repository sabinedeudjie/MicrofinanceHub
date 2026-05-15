package org.example.clientservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // ── Événements client ──────────────────────────────────────────────────────
    public static final String CLIENT_EXCHANGE        = "client.exchange";
    public static final String CLIENT_CREATED_QUEUE   = "client.created.queue";
    public static final String CLIENT_CREATED_KEY     = "client.created";

    @Value("${rabbitmq.queue.user-login}")
    private String userLoginQueue;

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.routing-key.user-login}")
    private String userLoginRoutingKey;

    @Bean
    public TopicExchange clientExchange() {
        return ExchangeBuilder.topicExchange(CLIENT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue clientCreatedQueue() {
        return QueueBuilder.durable(CLIENT_CREATED_QUEUE).build();
    }

    @Bean
    public Binding clientCreatedBinding() {
        return BindingBuilder.bind(clientCreatedQueue()).to(clientExchange()).with(CLIENT_CREATED_KEY);
    }

    @Bean
    public Queue userLoginQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", userEventsExchange + ".dlx");
        args.put("x-dead-letter-routing-key", userLoginRoutingKey + ".dlq");
        args.put("x-max-retries", 3);
        
        return QueueBuilder.durable(userLoginQueue)
                .withArguments(args)
                .build();
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return ExchangeBuilder.topicExchange(userEventsExchange)
                .durable(true)
                .build();
    }

    @Bean
    public Binding userLoginBinding() {
        return BindingBuilder
                .bind(userLoginQueue())
                .to(userEventsExchange())
                .with(userLoginRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}


//  org.example.clientservice.config;

//  com.fasterxml.jackson.databind.ObjectMapper;
//  com.fasterxml.jackson.databind.SerializationFeature;
//  com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//  org.springframework.amqp.core.*;
//  org.springframework.amqp.rabbit.connection.ConnectionFactory;
//  org.springframework.amqp.rabbit.core.RabbitTemplate;
//  org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//  org.springframework.amqp.support.converter.MessageConverter;
//  org.springframework.beans.factory.annotation.Value;
//  org.springframework.context.annotation.Bean;
//  org.springframework.context.annotation.Configuration;

// 
//  class RabbitMQConfig {

//     ("${rabbitmq.queue.user-login}")
//      String userLoginQueue;

//     
//      Queue userLoginQueue() {
//          QueueBuilder.durable(userLoginQueue)
//                 .("x-dead-letter-exchange", "user.events.dlx")
//                 .();
//     

//     
//      MessageConverter messageConverter() {
//          mapper = new ObjectMapper();
//         .registerModule(new JavaTimeModule());
//         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//          new Jackson2JsonMessageConverter(mapper);
//     

//     
//      RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//          template = new RabbitTemplate(connectionFactory);
//         .setMessageConverter(messageConverter());
//          template;
//     
// 