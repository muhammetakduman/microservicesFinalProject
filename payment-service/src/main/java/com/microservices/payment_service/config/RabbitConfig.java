package com.microservices.payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ yapılandırması.
 *
 * payment-service DİNLER:
 *   payment.triggered.queue  ← order-service
 *
 * payment-service PUBLISH EDER:
 *   payment.success.queue    → order-service
 *   payment.failed.queue     → order-service, stok-service
 *   notification.queue       → mail-service (notification.email routing key)
 */
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.payment-triggered:payment.triggered.queue}")
    private String paymentTriggeredQueue;

    @Value("${rabbitmq.queue.payment-success:payment.success.queue}")
    private String paymentSuccessQueue;

    @Value("${rabbitmq.queue.payment-failed:payment.failed.queue}")
    private String paymentFailedQueue;

    @Value("${rabbitmq.routing-key.payment-triggered:payment.triggered}")
    private String paymentTriggeredRoutingKey;

    @Value("${rabbitmq.routing-key.payment-success:payment.success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue paymentTriggeredQueue() {
        return QueueBuilder.durable(paymentTriggeredQueue).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(paymentSuccessQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue).build();
    }

    @Bean
    public Binding paymentTriggeredBinding() {
        return BindingBuilder.bind(paymentTriggeredQueue()).to(marketplaceExchange()).with(paymentTriggeredRoutingKey);
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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * @RabbitListener'ın JSON deserialize etmesi için container factory'ye
     * message converter set edilmesi zorunludur.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
