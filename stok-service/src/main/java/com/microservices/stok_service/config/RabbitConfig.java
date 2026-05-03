package com.microservices.stok_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.order-created:order.created.queue}")
    private String orderCreatedQueue;

    @Value("${rabbitmq.queue.payment-success:payment.success.queue}")
    private String paymentSuccessQueue;

    @Value("${rabbitmq.queue.payment-failed:payment.failed.queue}")
    private String paymentFailedQueue;

    @Value("${rabbitmq.queue.stock-reserved:stock.reserved.queue}")
    private String stockReservedQueue;

    @Value("${rabbitmq.queue.stock-not-available:stock.not-available.queue}")
    private String stockNotAvailableQueue;

    // Urun onay eventi
    private static final String PRODUCT_APPROVED_QUEUE       = "product.approved.queue";
    private static final String PRODUCT_APPROVED_ROUTING_KEY = "product.approved";

    @Value("${rabbitmq.routing-key.order-created:order.created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-success:payment.success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Value("${rabbitmq.routing-key.stock-reserved:stock.reserved}")
    private String stockReservedRoutingKey;

    @Value("${rabbitmq.routing-key.stock-not-available:stock.not-available}")
    private String stockNotAvailableRoutingKey;

    // Exchange
    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(exchange);
    }

    // Queues - stok-service'in consume ettiği
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(paymentSuccessQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue).build();
    }

    // Queues - stok-service'in publish ettiği
    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(stockReservedQueue).build();
    }

    @Bean
    public Queue stockNotAvailableQueue() {
        return QueueBuilder.durable(stockNotAvailableQueue).build();
    }

    // Bindings
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(marketplaceExchange()).with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(marketplaceExchange()).with(paymentSuccessRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(marketplaceExchange()).with(paymentFailedRoutingKey);
    }

    @Bean
    public Binding stockReservedBinding() {
        return BindingBuilder.bind(stockReservedQueue()).to(marketplaceExchange()).with(stockReservedRoutingKey);
    }

    @Bean
    public Binding stockNotAvailableBinding() {
        return BindingBuilder.bind(stockNotAvailableQueue()).to(marketplaceExchange()).with(stockNotAvailableRoutingKey);
    }

    @Bean
    public Queue productApprovedQueue() {
        return QueueBuilder.durable(PRODUCT_APPROVED_QUEUE).build();
    }

    @Bean
    public Binding productApprovedBinding() {
        return BindingBuilder.bind(productApprovedQueue()).to(marketplaceExchange()).with(PRODUCT_APPROVED_ROUTING_KEY);
    }

    // JSON converter
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

