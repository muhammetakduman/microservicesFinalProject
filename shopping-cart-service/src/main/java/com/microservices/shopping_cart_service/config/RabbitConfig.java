package com.microservices.shopping_cart_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ yapılandırması.
 *
 * cart-service yalnızca DİNLER:
 *   cart.clear.queue ← payment-service, PaymentSuccess event'inde publish eder.
 *   Sepet otomatik olarak temizlenir.
 */
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.cart-clear:cart.clear.queue}")
    private String cartClearQueue;

    @Value("${rabbitmq.routing-key.cart-clear:cart.clear}")
    private String cartClearRoutingKey;

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue cartClearQueue() {
        // durable = true → RabbitMQ yeniden başlasa da queue kaybolmaz
        return QueueBuilder.durable(cartClearQueue).build();
    }

    @Bean
    public Binding cartClearBinding() {
        return BindingBuilder.bind(cartClearQueue()).to(marketplaceExchange()).with(cartClearRoutingKey);
    }

    /** Mesajları JSON olarak serialize/deserialize et */
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
}

