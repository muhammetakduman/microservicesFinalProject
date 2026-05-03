package com.microservices.mail_service.config;

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
 * mail-service SADECE DİNLER: notification.queue
 */
@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange:marketplace.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.notification:notification.queue}")
    private String notificationQueue;

    @Value("${rabbitmq.routing-key.notification-email:notification.email}")
    private String notificationEmailRoutingKey;

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(marketplaceExchange()).with(notificationEmailRoutingKey);
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
