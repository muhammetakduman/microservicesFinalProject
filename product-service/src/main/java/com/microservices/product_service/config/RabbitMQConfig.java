package com.microservices.product_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "marketplace.exchange";
    public static final String STOCK_DECREASE_QUEUE = "stock.decrease.queue";
    public static final String STOCK_DECREASE_ROUTING_KEY = "stock.decrease";

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue stockDecreaseQueue() {
        return new Queue(STOCK_DECREASE_QUEUE, true);
    }

    @Bean
    public Binding stockDecreaseBinding(Queue stockDecreaseQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder
                .bind(stockDecreaseQueue)
                .to(marketplaceExchange)
                .with(STOCK_DECREASE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}

