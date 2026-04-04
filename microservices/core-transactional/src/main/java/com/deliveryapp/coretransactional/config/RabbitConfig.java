package com.deliveryapp.coretransactional.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // --- IDENTITY EXCHANGE ---
    public static final String EXCHANGE_NAME = "user.exchange";
    public static final String QUEUE_NAME = "wallet.creation.queue";
    public static final String ROUTING_KEY = "user.created";

    // --- LOGISTIC EXCHANGE ---
    public static final String LOGISTIC_EXCHANGE = "logistic.exchange";

    // Nombres de colas para los eventos logísticos
    public static final String ORDER_CREATED_QUEUE = "python.matchmaking.order.created.queue";
    public static final String ORDER_STATUS_QUEUE = "tracking.order.status.queue";
    public static final String DRIVER_PENALTY_QUEUE = "identity.driver.penalized.queue";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange logisticExchange() {
        return new TopicExchange(LOGISTIC_EXCHANGE);
    }

    // --- BINDINGS DE IDENTITY ---
    @Bean
    public Queue walletQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding bindingWallet(Queue walletQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(walletQueue).to(userExchange).with(ROUTING_KEY);
    }

    // --- BINDINGS LOGÍSTICOS ---

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Binding bindingOrderCreated(Queue orderCreatedQueue, TopicExchange logisticExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(logisticExchange).with("order.created");
    }

    @Bean
    public Queue orderStatusQueue() {
        return new Queue(ORDER_STATUS_QUEUE, true);
    }

    @Bean
    public Binding bindingOrderStatus(Queue orderStatusQueue, TopicExchange logisticExchange) {
        return BindingBuilder.bind(orderStatusQueue).to(logisticExchange).with("order.status.updated");
    }

    @Bean
    public Queue driverPenaltyQueue() {
        return new Queue(DRIVER_PENALTY_QUEUE, true);
    }

    @Bean
    public Binding bindingDriverPenalty(Queue driverPenaltyQueue, TopicExchange logisticExchange) {
        return BindingBuilder.bind(driverPenaltyQueue).to(logisticExchange).with("driver.penalized");
    }

    // --- CONVERSOR JSON ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}