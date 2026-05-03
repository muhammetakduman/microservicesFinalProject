package com.microservices.order_service.config;

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
 * Exchange    : marketplace.exchange (topic)
 *
 * order-service PUBLISH eder:
 *   order.created  → order.created.queue  (stok-service dinler)
 *
 * order-service DİNLER:
 *   stock.reserved       → stock.reserved.queue      (stok-service publish eder)
 *   stock.not-available  → stock.not-available.queue (stok-service publish eder)
 *   payment.success      → payment.success.queue     (payment-service publish eder)
 *   payment.failed       → payment.failed.queue      (payment-service publish eder)
 *
 * İleride eklenmesi beklenenler:
 *   payment.triggered → payment.triggered.queue (payment-service dinler)
 */
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    // -------- Queue isimleri --------
    @Value("${rabbitmq.queue.order-created:order.created.queue}")
    private String orderCreatedQueue;

    @Value("${rabbitmq.queue.stock-reserved:stock.reserved.queue}")
    private String stockReservedQueue;

    @Value("${rabbitmq.queue.stock-not-available:stock.not-available.queue}")
    private String stockNotAvailableQueue;

    @Value("${rabbitmq.queue.payment-success:payment.success.queue}")
    private String paymentSuccessQueue;

    @Value("${rabbitmq.queue.payment-failed:payment.failed.queue}")
    private String paymentFailedQueue;

    // -------- Routing key'ler --------
    @Value("${rabbitmq.routing-key.order-created:order.created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.stock-reserved:stock.reserved}")
    private String stockReservedRoutingKey;

    @Value("${rabbitmq.routing-key.stock-not-available:stock.not-available}")
    private String stockNotAvailableRoutingKey;

    @Value("${rabbitmq.routing-key.payment-success:payment.success}")
    private String paymentSuccessRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed:payment.failed}")
    private String paymentFailedRoutingKey;

    // ========================
    // Exchange
    // ========================

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(exchange);
    }

    // ========================
    // Queue tanımları
    // ========================

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(orderCreatedQueue).build();
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(stockReservedQueue).build();
    }

    @Bean
    public Queue stockNotAvailableQueue() {
        return QueueBuilder.durable(stockNotAvailableQueue).build();
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(paymentSuccessQueue).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(paymentFailedQueue).build();
    }

    // ========================
    // Binding (queue ↔ exchange ↔ routing key)
    // ========================

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(marketplaceExchange()).with(orderCreatedRoutingKey);
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
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(marketplaceExchange()).with(paymentSuccessRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(marketplaceExchange()).with(paymentFailedRoutingKey);
    }

    // ========================
    // JSON serializer — nesneleri JSON olarak gönder/al
    // ========================

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

