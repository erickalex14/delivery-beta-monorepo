package com.deliveryapp.coretransactional.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "wallet.creation.queue";
    public static final String EXCHANGE_NAME = "user.exchange";
    public static final String ROUTING_KEY = "user.created";

    @Bean
    public Queue walletQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue walletQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(walletQueue).to(userExchange).with(ROUTING_KEY);
    }

    //@Bean
    //public Jackson2JsonMessageConverter jsonMessageConverter() {
    //    return new Jackson2JsonMessageConverter();
    //}
}